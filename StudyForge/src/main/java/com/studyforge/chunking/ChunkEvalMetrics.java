package com.studyforge.chunking;

import lombok.Data;

/**
 * 分块质量指标——用数据对比不同策略，而不是拍脑袋。
 * 这是“把分块当一等公民”的另一个体现：分块是可评估、可对比的。
 */
@Data
public class ChunkEvalMetrics {

    private int blockCount;
    private double avgBlockLen;
    private int minBlockLen;
    private int maxBlockLen;
    private double sizeVariance;        // 块长离散度，越小越均匀
    private double avgTopicConsistency; // 块内主题一致性（0-1），越大越好
    private long totalChars;
    private double coverageRatio;       // 被分块覆盖的文本比例
}
