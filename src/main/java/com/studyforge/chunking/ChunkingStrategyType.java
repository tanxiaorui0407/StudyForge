package com.studyforge.chunking;

/**
 * 分块策略类型——三种策略都可插拔，用指标对比评估。
 * 这是本项目把“分块”当一等公民的核心：分块不再是不透明黑盒。
 */
public enum ChunkingStrategyType {
    FIXED_LENGTH,       // 固定长度 + 重叠窗口
    HEADING_BASED,      // 按标题层级切分
    SEMANTIC_BOUNDARY   // 语义边界（话题转移检测）
}
