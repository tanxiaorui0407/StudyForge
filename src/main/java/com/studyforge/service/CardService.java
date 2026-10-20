package com.studyforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.chunking.ChunkUnit;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.entity.KnowledgeCard;
import com.studyforge.llm.LlmClient;
import com.studyforge.llm.LlmException;
import com.studyforge.llm.LlmResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识卡片生成：每块产出一张（或 LLM 生成面试问法版）。
 *
 * 与 TreeMind 的差异点：不止导图，还给出“带面试问法的知识卡片” -> Anki 复习。
 *  LLM 可用：按块让 LLM 生成「问题面/答案面」；
 *  LLM 不可用：用模板兜底（问题面 = “请解释主题”，答案面 = 块内容摘要）。
 */
@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private static final String SYSTEM_CARD =
            "你是求职面试辅导。给定复习资料的一个片段，把它转成一张 Anki 知识卡片。" +
            "问题面要像面试官会问的开放问题，答案面覆盖核心要点。" +
            "只输出 JSON：{\"front\":\"...\",\"back\":\"...\",\"tags\":[\"tag1\",\"tag2\"]}";

    private final LlmClient llm;
    private final ObjectMapper mapper;
    private final StudyForgeProperties props;

    public CardService(LlmClient llm, ObjectMapper mapper, StudyForgeProperties props) {
        this.llm = llm;
        this.mapper = mapper;
        this.props = props;
    }

    /**
     * @param documentId 文档 id
     * @param chunks     分块
     * @param docTitle   文档标题（用于 tag）
     * @param maxCards   卡片上限
     */
    public List<KnowledgeCard> generate(Long documentId, List<ChunkUnit> chunks, String docTitle, int maxCards) {
        List<KnowledgeCard> cards = new ArrayList<>();
        int cap = Math.max(1, Math.min(maxCards, chunks.size()));
        for (int i = 0; i < cap; i++) {
            ChunkUnit c = chunks.get(i);
            KnowledgeCard card = llm.isEnabled()
                    ? llmCard(documentId, c, i)
                    : templateCard(documentId, c, i);
            if (card != null) {
                card.setDocumentId(documentId);
                card.setChunkId(null); // 分块尚未落库时置空；落库后再回填
                card.setTags(tagOf(docTitle, c));
                card.setSourceRef("第 " + (i + 1) + " 块" + (c.getTitle() == null ? "" : " / " + c.getTitle()));
                cards.add(card);
            }
        }
        return cards;
    }

    private KnowledgeCard llmCard(Long documentId, ChunkUnit c, int index) {
        try {
            String user = "片段" + (index + 1) + "：\n" + c.getContent();
            LlmResult r = llm.chat("CARD_GEN", SYSTEM_CARD, user);
            JsonNode node = extractJson(r.getContent());
            if (node == null || !node.has("front") || !node.has("back")) {
                throw new LlmException("parse", "卡片 JSON 缺字段");
            }
            KnowledgeCard card = new KnowledgeCard();
            card.setDocumentId(documentId);
            card.setFront(node.path("front").asText());
            card.setBack(node.path("back").asText());
            return card;
        } catch (RuntimeException e) {
            log.warn("卡片生成失败，用模板兜底（块 {}）: {}", index, e.getMessage());
            return templateCard(documentId, c, index);
        }
    }

    private KnowledgeCard templateCard(Long documentId, ChunkUnit c, int index) {
        String keyword = c.getKeywords() == null || c.getKeywords().isEmpty()
                ? (c.getTitle() == null ? "本段内容" : c.getTitle())
                : String.join("、", c.getKeywords().subList(0, Math.min(3, c.getKeywords().size())));
        KnowledgeCard card = new KnowledgeCard();
        card.setDocumentId(documentId);
        card.setFront("面试问法：请解释「" + keyword + "」的核心内容与要点。");
        card.setBack(truncate(c.getContent(), 600));
        return card;
    }

    private String tagOf(String docTitle, ChunkUnit c) {
        String tag = docTitle == null || docTitle.isBlank() ? "studyforge" : docTitle.trim().replaceAll("\\s+", "-");
        if (c.getKeywords() != null && !c.getKeywords().isEmpty()) {
            tag = tag + " " + String.join(" ", c.getKeywords().subList(0, Math.min(3, c.getKeywords().size())));
        }
        return tag;
    }

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

    private String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() > n ? s.substring(0, n) : s;
    }
}
