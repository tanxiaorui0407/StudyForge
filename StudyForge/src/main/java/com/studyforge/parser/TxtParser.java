package com.studyforge.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 纯文本解析。
 */
@Component
public class TxtParser implements DocumentParser {

    private final HeadingExtractor headingExtractor;

    public TxtParser(HeadingExtractor headingExtractor) {
        this.headingExtractor = headingExtractor;
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        return fileName.toLowerCase().endsWith(".txt");
    }

    @Override
    public ParsedDocument parse(byte[] bytes, String fileName) throws IOException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        return headingExtractor.build(text, fileName, false);
    }
}
