package com.studyforge.parser;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析器注册表：按文件类型分发到具体解析器。
 */
@Component
public class ParserRegistry {

    private final List<DocumentParser> parsers;

    public ParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public DocumentParser resolve(String contentType, String fileName) {
        return parsers.stream()
                .filter(p -> p.supports(contentType, fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型: " + fileName));
    }
}
