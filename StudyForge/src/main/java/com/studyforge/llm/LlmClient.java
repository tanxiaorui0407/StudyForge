package com.studyforge.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.reliability.ApiLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 LLM 客户端。
 * 兼容 DeepSeek / OpenAI / 通义等任意 OpenAI 协议接口：
 *   POST {baseUrl}/chat/completions，body 含 model / messages / max_tokens。
 * 每次调用都会写入 api_call_log（token / 耗时 / 失败原因）——成本与错误都可追溯。
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final StudyForgeProperties props;
    private final ObjectMapper mapper;
    private final ApiLogService apiLogService;
    private final HttpClient http;

    public LlmClient(StudyForgeProperties props, ObjectMapper mapper, ApiLogService apiLogService) {
        this.props = props;
        this.mapper = mapper;
        this.apiLogService = apiLogService;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 是否已配置可用（未配置则整个系统自动降级为标题兜底） */
    public boolean isEnabled() {
        StudyForgeProperties.Llm cfg = props.getLlm();
        return cfg.isEnabled() && cfg.getApiKey() != null && !cfg.getApiKey().isBlank();
    }

    /**
     * 发起一次对话补全。
     *
     * @param operation 操作名（写入日志，如 OUTLINE_MAP / OUTLINE_REDUCE）
     * @param system    系统提示
     * @param user      用户输入
     */
    public LlmResult chat(String operation, String system, String user) {
        StudyForgeProperties.Llm cfg = props.getLlm();
        long start = System.currentTimeMillis();
        String content = "";
        long inputTokens = 0, outputTokens = 0;
        try {
            Map<String, Object> body = Map.of(
                    "model", cfg.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)),
                    "temperature", 0.2,
                    "max_tokens", cfg.getMaxTokens()
            );
            String json = mapper.writeValueAsString(body);
            String url = cfg.getBaseUrl().endsWith("/chat/completions")
                    ? cfg.getBaseUrl() : cfg.getBaseUrl() + "/chat/completions";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                apiLogService.record(operation, cfg.getModel(), 0, 0,
                        System.currentTimeMillis() - start, false, "http",
                        "HTTP " + resp.statusCode() + ": " + truncate(resp.body(), 300));
                throw new LlmException("http", "HTTP " + resp.statusCode());
            }

            JsonNode root = mapper.readTree(resp.body());
            content = root.path("choices").path(0).path("message").path("content").asText("");
            inputTokens = root.path("usage").path("prompt_tokens").asLong(0);
            outputTokens = root.path("usage").path("completion_tokens").asLong(0);

            long latency = System.currentTimeMillis() - start;
            apiLogService.record(operation, cfg.getModel(), (int) inputTokens, (int) outputTokens,
                    latency, true, null, null);
            log.info("[LLM] {} ok in {}ms, in={} out={}", operation, latency, inputTokens, outputTokens);

            if (content.isBlank()) {
                throw new LlmException("empty", "LLM 返回空内容");
            }
            return toResult(cfg.getModel(), content, inputTokens, outputTokens, latency);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            apiLogService.record(operation, cfg.getModel(), 0, 0,
                    System.currentTimeMillis() - start, false, "network", truncate(e.getMessage(), 300));
            throw new LlmException("network", "LLM 调用失败: " + e.getMessage(), e);
        }
    }

    private LlmResult toResult(String model, String content, long in, long out, long latency) {
        LlmResult r = new LlmResult();
        r.setModel(model);
        r.setContent(content);
        r.setInputTokens(in);
        r.setOutputTokens(out);
        r.setLatencyMs(latency);
        return r;
    }

    private String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() > n ? s.substring(0, n) : s;
    }
}
