package com.studyforge.dto;

import com.studyforge.chunking.ChunkEvalMetrics;
import lombok.Data;

import java.util.List;

/**
 * 单策略的分块评估结果（对比页用）。
 */
@Data
public class StrategyEvalResponse {

    private String strategy;
    private ChunkEvalMetrics metrics;
    private List<String> chunkTitles; // 前若干块标题，供人工抽查
    private int sampleBlocks;
}
