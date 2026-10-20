package com.studyforge.entity;

/**
 * 文档处理状态（流水线各阶段 + 终态）。
 */
public enum DocumentStatus {
    PENDING,      // 已上传，等待处理
    PARSING,      // 解析文本
    CHUNKING,     // 智能分块
    OUTLINING,    // Map-Reduce 生成大纲
    CARDING,      // 生成知识卡片
    SUCCEEDED,    // 全部完成
    FAILED        // 失败
}
