package com.studyforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文档摘要（列表用）。
 */
@Data
@AllArgsConstructor
public class DocumentSummaryResponse {

    private Long id;
    private String fileName;
    private Long sizeBytes;
    private String status;
    private String outlineMode;
    private Integer chunkCount;
    private Integer cardCount;
    private Long tokenUsed;
}
