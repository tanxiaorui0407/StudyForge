package com.studyforge.dto;

import lombok.Data;

/**
 * 大纲节点模型。既是 LLM 返回的 JSON Schema 结构，也是 markmap 渲染的数据结构。
 * 结构：{ title, children: [...] }，children 可选。
 */
@Data
public class OutlineNode {

    private String title;
    private java.util.List<OutlineNode> children;

    public OutlineNode() {
    }

    public OutlineNode(String title) {
        this.title = title;
    }
}
