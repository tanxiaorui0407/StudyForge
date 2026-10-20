package com.studyforge.parser;

import java.io.IOException;

/**
 * 文档解析器接口——按文件类型可插拔。
 */
public interface DocumentParser {

    /** 是否支持该类型（contentType 或扩展名判定） */
    boolean supports(String contentType, String fileName);

    /** 把文件字节解析为文本 + 标题层级 */
    ParsedDocument parse(byte[] bytes, String fileName) throws IOException;
}
