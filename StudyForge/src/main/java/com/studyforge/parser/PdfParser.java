package com.studyforge.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF 解析：使用 Apache PDFBox 提取文本，再走标题推断。
 */
@Component
public class PdfParser implements DocumentParser {

    private final HeadingExtractor headingExtractor;

    public PdfParser(HeadingExtractor headingExtractor) {
        this.headingExtractor = headingExtractor;
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".pdf") || (contentType != null && contentType.contains("pdf"));
    }

    @Override
    public ParsedDocument parse(byte[] bytes, String fileName) throws IOException {
        String text;
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(doc);
        }
        return headingExtractor.build(text, fileName, false);
    }
}
