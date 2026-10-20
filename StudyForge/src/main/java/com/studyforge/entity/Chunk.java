package com.studyforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分块：文档切分后的语义单元，知识卡片以块为单位生成。
 */
@Data
@TableName("chunk")
public class Chunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Integer chunkIndex;

    /** {@link com.studyforge.chunking.ChunkingStrategyType} */
    private String strategy;

    private Integer startOffset;
    private Integer endOffset;
    private String title;
    private String content;
    private String keywords;   // 块内关键词，用于展示与主题一致性评估
    private LocalDateTime createdAt;
}
