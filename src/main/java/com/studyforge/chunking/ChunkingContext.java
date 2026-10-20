package com.studyforge.chunking;

import lombok.Data;

import java.util.List;

/**
 * 分块上下文：从配置读入，传给各策略，避免策略直接依赖 Spring 配置类。
 */
@Data
public class ChunkingContext {

    private int fixedSize;    // 固定长度
    private int overlap;      // 重叠
    private int maxBlocks;    // 上限
    private int minBlockLen;  // 最小块长

    public ChunkingContext(int fixedSize, int overlap, int maxBlocks, int minBlockLen) {
        this.fixedSize = fixedSize;
        this.overlap = overlap;
        this.maxBlocks = maxBlocks;
        this.minBlockLen = minBlockLen;
    }

    /** 从配置构造 */
    public static ChunkingContext fromProperties(int fixedSize, int overlap, int maxBlocks, int minBlockLen) {
        return new ChunkingContext(fixedSize, overlap, maxBlocks, minBlockLen);
    }
}
