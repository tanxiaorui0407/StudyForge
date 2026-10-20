package com.studyforge.parser;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析产物：一份资料解析后的纯文本 + 推断出的标题层级。
 * 标题信息同时服务于“按标题层级分块”和“标题降级兜底大纲”。
 */
@Data
public class ParsedDocument {

    private String title;          // 推断标题（优先首个大标题，其次文件名）
    private String fullText;       // 完整文本
    private List<Heading> headings = new ArrayList<>(); // 按出现顺序

    @Data
    public static class Heading {
        private String title;
        private int level;          // 1 为最高级
        private int startOffset;    // 在 fullText 中的起始偏移
        private int endOffset;      // 到下一个标题为止（最后一个到文本末尾）
    }
}
