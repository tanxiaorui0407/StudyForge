package com.studyforge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档记录：一次上传的一份资料。
 */
@Data
@TableName("document")
public class DocumentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private String fileHash;      // 文件内容 MD5
    private Long sizeBytes;
    private String contentType;

    /** {@link DocumentStatus} */
    private String status;

    /** 层级大纲 JSON（前端 markmap 直接渲染） */
    private String outlineJson;

    /** {@link OutlineMode} */
    private String outlineMode;

    /** 解析后的全文（用于策略对比、卡片回溯） */
    private String fullText;

    /** 该文档累计消耗 token（可靠性/成本可见性） */
    private Long tokenUsed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
