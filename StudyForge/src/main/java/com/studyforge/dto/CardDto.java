package com.studyforge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 知识卡片 DTO。
 */
@Data
@AllArgsConstructor
public class CardDto {

    private Long id;
    private String front;
    private String back;
    private String tags;
    private String sourceRef;
}
