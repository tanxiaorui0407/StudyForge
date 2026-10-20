package com.studyforge.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Markdown 解析：按 "# " 前缀识别标题层级，更精确。
 */
@Component
public class MarkdownParser implements DocumentParser {

    private final HeadingExtractor headingExtractor;

    public MarkdownParser(HeadingExtractor headingExtractor) {
        this.headingExtractor = headingExtractor;
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".md") || name.endsWith(".markdown");
    }

    @Override
    public ParsedDocument parse(byte[] bytes, String fileName) throws IOException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        return headingExtractor.build(text, fileName, true);
    }
}
