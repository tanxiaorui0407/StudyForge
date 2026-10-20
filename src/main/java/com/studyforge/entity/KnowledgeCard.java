package com.studyforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识卡片：正面问题、背面答案，可导出为 Anki 格式。
 * “面试问法”体现在 front 由模板生成，如“请解释【主题】是什么？”
 */
@Data
@TableName("knowledge_card")
public class KnowledgeCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Long chunkId;

    private String front;      // 问题面
    private String back;       // 答案面
    private String tags;
    private String sourceRef;  // 例如 "第 3 块 / 章节标题"
    private LocalDateTime createdAt;
}
