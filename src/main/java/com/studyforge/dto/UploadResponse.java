package com.studyforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 上传接口返回：上传即返回 taskId，前端轮询任务状态。
 */
@Data
@AllArgsConstructor
public class UploadResponse {

    private Long taskId;      // 用于轮询 /api/tasks/{id}
    private Long documentId;
    private String status;    // PENDING
    private boolean reused;   // 是否命中文件哈希缓存（复用历史结果）
}
