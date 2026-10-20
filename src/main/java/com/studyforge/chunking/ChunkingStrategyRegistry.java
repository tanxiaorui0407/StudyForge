package com.studyforge.chunking;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 分块策略注册表：持有全部已注册策略，按类型取用。
 * 这是“策略可插拔”的落地：新增策略只需作为 Spring Bean 注册，这里自动收集。
 */
@Component
public class ChunkingStrategyRegistry {

    private final Map<ChunkingStrategyType, ChunkingStrategy> strategies = new EnumMap<>(ChunkingStrategyType.class);

    public ChunkingStrategyRegistry(List<ChunkingStrategy> all) {
        for (ChunkingStrategy s : all) {
            strategies.put(s.type(), s);
        }
    }

    public ChunkingStrategy get(ChunkingStrategyType type) {
        ChunkingStrategy s = strategies.get(type);
        if (s == null) {
            throw new IllegalArgumentException("未注册的分块策略: " + type);
        }
        return s;
    }

    public List<ChunkingStrategy> all() {
        return strategies.values().stream().toList();
    }
}
