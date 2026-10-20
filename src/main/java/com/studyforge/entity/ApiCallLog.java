package com.studyforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 每次 LLM 调用的记录：token、耗时、成功/失败与失败原因。
 * 这是“可靠性套件”的可观测性基础——成本与错误都可视、可追溯。
 */
@Data
@TableName("api_call_log")
public class ApiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operation;   // CHUNK_MAP / OUTLINE_REDUCE / CARD_GEN / ...
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private Boolean success;
    private String errorType;
    private String errorMessage;
    private LocalDateTime createdAt;
}
