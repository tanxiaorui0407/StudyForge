package com.studyforge.llm;

import lombok.Data;

/**
 * 一次 LLM 调用的结果。
 */
@Data
public class LlmResult {

    private String content;
    private long inputTokens;
    private long outputTokens;
    private long latencyMs;
    private String model;
}
