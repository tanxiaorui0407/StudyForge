package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略一：固定长度 + 重叠窗口。
 * 最简单、最确定（同一文件必得同一结果），是哈希缓存友好的基线策略。
 */
@Component
public class FixedLengthStrategy implements ChunkingStrategy {

    private final TextUtil textUtil;

    public FixedLengthStrategy(TextUtil textUtil) {
        this.textUtil = textUtil;
    }

    @Override
    public ChunkingStrategyType type() {
        return ChunkingStrategyType.FIXED_LENGTH;
    }

    @Override
    public List<ChunkUnit> chunk(ParsedDocument doc, ChunkingContext ctx) {
        String text = doc.getFullText();
        int size = ctx.getFixedSize();
        int overlap = Math.min(ctx.getOverlap(), size / 2);
        int step = Math.max(1, size - overlap);

        List<ChunkUnit> units = new ArrayList<>();
        int start = 0;
        int maxBlocks = ctx.getMaxBlocks();
        while (start < text.length() && units.size() < maxBlocks) {
            int end = Math.min(text.length(), start + size);
            String content = text.substring(start, end);
            ChunkUnit u = new ChunkUnit();
            u.setIndex(units.size());
            u.setContent(content.trim());
            u.setStartOffset(start);
            u.setEndOffset(end);
            u.setTitle("片段 " + (units.size() + 1));
            u.setKeywords(textUtil.keywords(content, 6));
            units.add(u);
            if (end >= text.length()) {
                break;
            }
            start += step;
        }
        return mergeTiny(units, ctx);
    }

    /** 过短块并入前一块（策略参数 minBlockLen） */
    protected List<ChunkUnit> mergeTiny(List<ChunkUnit> units, ChunkingContext ctx) {
        if (units.size() <= 1 || ctx.getMinBlockLen() <= 0) {
            return units;
        }
        List<ChunkUnit> merged = new ArrayList<>();
        for (ChunkUnit u : units) {
            if (!merged.isEmpty() && u.getContent().length() < ctx.getMinBlockLen()) {
                ChunkUnit last = merged.get(merged.size() - 1);
                last.setContent(last.getContent() + "\n" + u.getContent());
                last.setEndOffset(u.getEndOffset());
            } else {
                merged.add(u);
            }
        }
        return merged;
    }
}
