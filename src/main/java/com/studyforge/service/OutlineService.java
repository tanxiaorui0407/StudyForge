package com.studyforge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.chunking.ChunkUnit;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.dto.OutlineNode;
import com.studyforge.entity.OutlineMode;
import com.studyforge.llm.LlmClient;
import com.studyforge.llm.LlmException;
import com.studyforge.llm.LlmResult;
import com.studyforge.parser.ParsedDocument;
import com.studyforge.reliability.FallbackOutlineGenerator;
import com.studyforge.reliability.OutlineSchemaValidator;
import com.studyforge.reliability.RetryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 大纲生成（Map-Reduce）——本项目区别于“调一次 API 画个图”的关键工程点。
 *
 * 为什么不能把长文档一次塞给 LLM？
 *   上下文有上限，几十页 PDF 会爆 context / 烧大量 token / 丢失细节。
 * 做法：
 *   MAP   阶段：逐块让 LLM 提炼“小结 + 关键词”（每块独立、可并行）；
 *   REDUCE 阶段：把所有小结合并，让 LLM 生成层级大纲（JSON）；
 *   可靠性：JSON Schema 校验 -> 失败重试 -> 仍失败则标题兜底。
 */
@Service
public class OutlineService {

    private static final Logger log = LoggerFactory.getLogger(OutlineService.class);

    private static final String SYSTEM_MAP =
            "你是复习资料拆解助手。给定资料的一个片段，用一句话概括它的主题，并列出不超过5个关键词。" +
            "只输出 JSON：{\"summary\":\"...\",\"keywords\":[\"...\"]}";

    private static final String SYSTEM_REDUCE =
            "你是复习资料拆解助手。下面是该资料各片段的小结列表。请把它们组织成一份层级清晰的学习大纲。" +
            "只输出 JSON，结构为：{\"title\":\"资料标题\",\"children\":[{\"title\":\"章节\",\"children\":[{\"title\":\"小节\"}]}]}" +
            "要求：所有节点都必须有 title；层级不超过6层；节点总数不超过80个。不要输出任何解释文字。";

    private final LlmClient llm;
    private final ObjectMapper mapper;
    private final RetryRunner retryRunner;
    private final OutlineSchemaValidator validator;
    private final FallbackOutlineGenerator fallback;
    private final StudyForgeProperties props;

    public OutlineService(LlmClient llm, ObjectMapper mapper, RetryRunner retryRunner,
                          OutlineSchemaValidator validator, FallbackOutlineGenerator fallback,
                          StudyForgeProperties props) {
        this.llm = llm;
        this.mapper = mapper;
        this.retryRunner = retryRunner;
        this.validator = validator;
        this.fallback = fallback;
        this.props = props;
    }

    /** 大纲生成结果 */
    public static class OutlineResult {
        public OutlineNode root;
        public OutlineMode mode;
        public long tokenUsed;
    }

    /**
     * 主入口：优先 Map-Reduce（LLM），失败/未配置自动降级为标题兜底。
     *
     * @param doc   解析后的文档
     * @param chunkTitles 分块单元（兜底时作为叶子展示）
     */
    public OutlineResult generateOutline(ParsedDocument doc, List<ChunkUnit> chunkTitles) {
        OutlineResult result = new OutlineResult();
        if (!llm.isEnabled()) {
            log.info("LLM 未配置，使用标题兜底大纲");
            result.root = fallback.generate(doc, chunkTitles);
            result.mode = OutlineMode.FALLBACK_HEADING;
            result.tokenUsed = 0;
            return result;
        }

        long[] tokens = {0};
        try {
            // ---------- MAP：逐块小结 ----------
            List<String> summaries = retryRunner.run(props.getOutline().getMapReduceRetries(), 500, () -> {
                List<String> maps = new ArrayList<>();
                for (int i = 0; i < chunkTitles.size(); i++) {
                    String user = "第" + (i + 1) + "个片段（共" + chunkTitles.size() + "个）：\n" + chunkTitles.get(i).getContent();
                    LlmResult r = llm.chat("OUTLINE_MAP", SYSTEM_MAP, user);
                    tokens[0] += r.getInputTokens() + r.getOutputTokens();
                    JsonNode node = extractJson(r.getContent());
                    if (node == null || !node.has("summary")) {
                        throw new LlmException("parse", "MAP 返回缺少 summary");
                    }
                    maps.add(node.path("summary").asText());
                }
                return maps;
            });

            // ---------- REDUCE：合并为层级大纲 ----------
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < summaries.size(); i++) {
                sb.append(i + 1).append(". ").append(summaries.get(i)).append("\n");
            }

            OutlineNode root = retryRunner.run(props.getOutline().getMapReduceRetries(), 1000, () -> {
                LlmResult r = llm.chat("OUTLINE_REDUCE", SYSTEM_REDUCE, sb.toString());
                tokens[0] += r.getInputTokens() + r.getOutputTokens();
                try {
                    OutlineNode node = mapper.readValue(extractJson(r.getContent()).toString(), OutlineNode.class);
                    OutlineSchemaValidator.ValidationResult v = validator.validate(node, props.getOutline().getMaxOutlineNodes());
                    if (!v.valid) {
                        throw new LlmException("schema", "大纲 JSON 校验失败: " + v.errors);
                    }
                    return node;
                } catch (JsonProcessingException je) {
                    throw new LlmException("parse", "大纲 JSON 解析失败: " + je.getMessage());
                }
            });

            result.root = root;
            result.mode = OutlineMode.LLM_MAP_REDUCE;
            result.tokenUsed = tokens[0];
            return result;
        } catch (RuntimeException e) {
            log.warn("Map-Reduce 大纲失败（{}，{}），降级为标题兜底",
                    e.getClass().getSimpleName(), e.getMessage());
            result.root = fallback.generate(doc, chunkTitles);
            result.mode = OutlineMode.FALLBACK_HEADING;
            result.tokenUsed = 0;
            return result;
        }
    }

    /** 从 LLM 回复中稳健提取 JSON（容忍前后缀文字/代码块） */
    private JsonNode extractJson(String content) {
        try {
            String t = content.trim();
            if (t.startsWith("```")) {
                t = t.replaceFirst("^```(?:json)?", "").replaceAll("```$", "").trim();
            }
            return mapper.readTree(t);
        } catch (Exception e) {
            int s = content.indexOf('{');
            int e2 = content.lastIndexOf('}');
            if (s >= 0 && e2 > s) {
                try {
                    return mapper.readTree(content.substring(s, e2 + 1));
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }
}
