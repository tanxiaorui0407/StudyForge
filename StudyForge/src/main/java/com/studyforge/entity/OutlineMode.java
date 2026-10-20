package com.studyforge.entity;

/**
 * 大纲生成方式：用来标记导图是 LLM 生成还是标题降级兜底。
 */
public enum OutlineMode {
    LLM_MAP_REDUCE,     // 走了 Map-Reduce + JSON 校验
    FALLBACK_HEADING    // LLM 不可用/失败，降级为纯标题层级解析
}
