package com.studyforge.chunking;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分块单元：ChunkingStrategy 的产出（轻量、不落库），
 * 由服务层统一转成持久化的 Chunk 实体。
 */
@Data
public class ChunkUnit {

    private int index;           // 块序号，从 0 开始
    private String title;        // 块标题（标题策略取章节名，其他策略取关键词拼接）
    private String content;
    private int startOffset;
    private int endOffset;
    private List<String> keywords = new ArrayList<>();
}
