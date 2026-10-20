package com.studyforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 任务状态响应：前端据此展示进度条。
 */
@Data
@AllArgsConstructor
public class TaskStatusResponse {

    private Long taskId;
    private Long documentId;
    private String stage;     // PARSE/CHUNK/OUTLINE/CARD
    private String status;    // RUNNING/SUCCEEDED/FAILED
    private Integer progress; // 0-100
    private String errorMessage;
}
