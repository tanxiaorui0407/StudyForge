package com.studyforge.llm;

/**
 * LLM 调用异常，携带失败类型，便于记录与重试判断。
 */
public class LlmException extends RuntimeException {

    /** 失败类型：network(网络) / http(非200) / parse(解析失败) / empty(空回复) */
    private final String errorType;

    public LlmException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public LlmException(String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
