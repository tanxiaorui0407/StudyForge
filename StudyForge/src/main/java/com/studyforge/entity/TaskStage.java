package com.studyforge.entity;

/**
 * 任务阶段：上传后按顺序执行解析 -> 分块 -> 大纲 -> 卡片。
 */
public enum TaskStage {
    PARSE,   // 解析文本
    CHUNK,   // 智能分块
    OUTLINE, // 生成大纲
    CARD     // 生成知识卡片
}
