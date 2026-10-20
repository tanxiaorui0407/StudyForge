package com.studyforge.dto;

import lombok.Data;

import java.util.List;

/**
 * 文档详情：导图 + 分块 + 卡片。
 */
@Data
public class DocumentDetailResponse {

    private Long id;
    private String fileName;
    private String status;
    private String outlineMode;
    private String createdAt;

    private OutlineNode outline;
    private List<ChunkDto> chunks;
    private List<CardDto> cards;
}
