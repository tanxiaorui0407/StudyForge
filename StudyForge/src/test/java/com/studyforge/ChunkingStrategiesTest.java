package com.studyforge;

import com.studyforge.chunking.ChunkUnit;
import com.studyforge.chunking.ChunkingContext;
import com.studyforge.chunking.FixedLengthStrategy;
import com.studyforge.chunking.HeadingBasedStrategy;
import com.studyforge.chunking.SemanticBoundaryStrategy;
import com.studyforge.chunking.TextUtil;
import com.studyforge.parser.HeadingExtractor;
import com.studyforge.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingStrategiesTest {

    private final TextUtil textUtil = new TextUtil();
    private final HeadingExtractor extractor = new HeadingExtractor();
    private final ChunkingContext ctx = new ChunkingContext(80, 10, 50, 20);

    private ParsedDocument md(String content) {
        return extractor.build(content, "x.md", true);
    }

    @Test
    void fixedLengthCreatesExpectedBlocksWithOffsets() {
        String text = "一二三四五六七八九十".repeat(20); // 200 字符
        ParsedDocument doc = md(text);
        List<ChunkUnit> units = new FixedLengthStrategy(textUtil).chunk(doc, ctx);
        assertThat(units).isNotEmpty();
        assertThat(units.get(0).getStartOffset()).isEqualTo(0);
        // 相邻块应有重叠（start 递增小于 size）
        assertThat(units.get(1).getStartOffset()).isLessThan(units.get(0).getStartOffset() + 80);
        // 全文基本被覆盖
        int covered = units.stream().mapToInt(u -> u.getContent().length()).sum();
        assertThat(covered).isGreaterThanOrEqualTo(text.length() * 9 / 10);
    }

    @Test
    void headingBasedUsesChapterTitles() {
        ParsedDocument doc = md("# 第一章 概述\n内容甲。\n# 第二章 原理\n内容乙。");
        List<ChunkUnit> units = new HeadingBasedStrategy(textUtil, new FixedLengthStrategy(textUtil)).chunk(doc, ctx);
        assertThat(units).hasSize(2);
        assertThat(units.get(0).getTitle()).contains("第一章");
        assertThat(units.get(1).getTitle()).contains("第二章");
    }

    @Test
    void headingBasedFallsBackToFixedWhenNoHeading() {
        ParsedDocument doc = md("没有标题的正文，纯文本一段接一段。" .repeat(20));
        List<ChunkUnit> units = new HeadingBasedStrategy(textUtil, new FixedLengthStrategy(textUtil)).chunk(doc, ctx);
        assertThat(units).isNotEmpty();
    }

    @Test
    void semanticBoundaryProducesReasonableBlocks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append("计算机网络中的TCP协议负责可靠传输。TCP通过三次握手建立连接。滑动窗口控制流量。拥塞控制避免网络过载。").append(' ');
            sb.append("操作系统管理进程与内存。进程调度算法包括时间片轮转。虚拟内存提供地址隔离。死锁需要预防与避免。").append(' ');
        }
        ParsedDocument doc = md(sb.toString());
        List<ChunkUnit> units = new SemanticBoundaryStrategy(textUtil, new FixedLengthStrategy(textUtil)).chunk(doc, ctx);
        assertThat(units).isNotEmpty();
        assertThat(units.get(0).getContent()).isNotBlank();
    }

    @Test
    void chunksCarryKeywords() {
        ParsedDocument doc = md("# 标题\n计算机网络的TCP协议是传输层的核心。TCP保证数据有序可靠到达。");
        List<ChunkUnit> units = new SemanticBoundaryStrategy(textUtil, new FixedLengthStrategy(textUtil)).chunk(doc, ctx);
        assertThat(units).isNotEmpty();
        assertThat(units.get(0).getKeywords()).isNotEmpty();
    }
}
