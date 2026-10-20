package com.studyforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个分块的信息（返回给前端展示）。
 */
@Data
@AllArgsConstructor
public class ChunkDto {

    private Long id;
    private Integer index;
    private String title;
    private String keywords;
    private String content;
}
