package com.studyforge;

import com.studyforge.parser.HeadingExtractor;
import com.studyforge.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeadingExtractorTest {

    private final HeadingExtractor extractor = new HeadingExtractor();

    @Test
    void detectsMarkdownHeadings() {
        String md = "# 第一章 概述\n正文1。\n## 1.1 背景\n正文2。\n### 1.1.1 动机\n正文3。";
        ParsedDocument doc = extractor.build(md, "x.md", true);
        assertThat(doc.getHeadings()).hasSize(3);
        assertThat(doc.getHeadings().get(0).getTitle()).isEqualTo("第一章 概述");
        assertThat(doc.getHeadings().get(0).getLevel()).isEqualTo(1);
        assertThat(doc.getHeadings().get(1).getTitle()).isEqualTo("1.1 背景");
        assertThat(doc.getHeadings().get(1).getLevel()).isEqualTo(2);
        assertThat(doc.getHeadings().get(2).getLevel()).isEqualTo(3);
    }

    @Test
    void headingOffsetsAreMonotonic() {
        String md = "# A\nbody1\n## B\nbody2";
        ParsedDocument doc = extractor.build(md, "x.md", true);
        assertThat(doc.getHeadings().get(0).getStartOffset()).isLessThan(doc.getHeadings().get(1).getStartOffset());
        assertThat(doc.getHeadings().get(1).getEndOffset()).isEqualTo(md.length());
    }

    @Test
    void plainTextHeuristicHeading() {
        String txt = "这是普通正文的一句话，比较长所以不是标题。\n短行标题\n内容跟在这后面。";
        ParsedDocument doc = extractor.build(txt, "x.txt", false);
        assertThat(doc.getHeadings()).hasSize(1);
        assertThat(doc.getHeadings().get(0).getTitle()).isEqualTo("短行标题");
    }

    @Test
    void infersTitleFromDocTitle() {
        String md = "# 我的笔记\n内容";
        ParsedDocument doc = extractor.build(md, "随便.txt", true);
        assertThat(doc.getTitle()).isEqualTo("我的笔记");
    }
}
