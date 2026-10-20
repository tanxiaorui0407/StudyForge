package com.studyforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务：上传即返回 taskId，前端据此轮询状态。
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    /** {@link TaskStage} */
    private String stage;

    /** {@link TaskStatus} */
    private String status = TaskStatus.RUNNING.name();

    private Integer progress;   // 0-100
    private String errorMessage;
    private Long tokenUsed;
    private Long durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
