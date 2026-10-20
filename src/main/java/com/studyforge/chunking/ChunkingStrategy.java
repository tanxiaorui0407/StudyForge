package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;

import java.util.List;

/**
 * 分块策略接口——可插拔的一等公民。
 * 新增一种策略：实现本接口并注册进 ChunkingStrategyRegistry 即可。
 */
public interface ChunkingStrategy {

    ChunkingStrategyType type();

    List<ChunkUnit> chunk(ParsedDocument doc, ChunkingContext ctx);
}
