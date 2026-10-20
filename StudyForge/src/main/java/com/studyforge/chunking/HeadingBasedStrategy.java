package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略二：按标题层级切分。
 * 文档天然以章节为语义边界；标题策略直接切到标题上，块标题即为章节名，可解释性最强。
 * 若文档没有可识别标题，则回退到固定长度策略。
 */
@Component
public class HeadingBasedStrategy implements ChunkingStrategy {

    private final TextUtil textUtil;
    private final FixedLengthStrategy fallback;

    public HeadingBasedStrategy(TextUtil textUtil, FixedLengthStrategy fallback) {
        this.textUtil = textUtil;
        this.fallback = fallback;
    }

    @Override
    public ChunkingStrategyType type() {
        return ChunkingStrategyType.HEADING_BASED;
    }

    @Override
    public List<ChunkUnit> chunk(ParsedDocument doc, ChunkingContext ctx) {
        List<ParsedDocument.Heading> headings = doc.getHeadings();
        if (headings == null || headings.isEmpty()) {
            // 没有标题 -> 回退固定长度，保证可用
            return fallback.chunk(doc, ctx);
        }

        String text = doc.getFullText();
        List<ChunkUnit> units = new ArrayList<>();
        int idx = 0;
        for (ParsedDocument.Heading h : headings) {
            if (idx >= ctx.getMaxBlocks()) {
                break;
            }
            int start = Math.max(0, h.getStartOffset());
            int end = Math.min(text.length(), h.getEndOffset());
            String content = text.substring(start, end).trim();
            if (content.isEmpty()) {
                continue; // 空标题段跳过
            }
            ChunkUnit u = new ChunkUnit();
            u.setIndex(units.size());
            u.setTitle(h.getTitle());
            u.setContent(content);
            u.setStartOffset(start);
            u.setEndOffset(end);
            u.setKeywords(textUtil.keywords(content, 6));
            units.add(u);
            idx++;
        }

        // 若标题太少导致块过大，按固定长度再细分，保证块大小可控
        units = subdivideOversized(units, ctx);
        return units;
    }

    /** 若某一块过长（>3 倍固定长度），按标题下再切，保持块可控 */
    private List<ChunkUnit> subdivideOversized(List<ChunkUnit> units, ChunkingContext ctx) {
        List<ChunkUnit> result = new ArrayList<>();
        int maxLen = ctx.getFixedSize() * 3;
        for (ChunkUnit u : units) {
            if (u.getContent().length() <= maxLen) {
                result.add(u);
                continue;
            }
            int size = ctx.getFixedSize();
            int start = 0;
            String content = u.getContent();
            while (start < content.length() && result.size() < ctx.getMaxBlocks()) {
                int end = Math.min(content.length(), start + size);
                ChunkUnit sub = new ChunkUnit();
                sub.setIndex(result.size());
                sub.setTitle(u.getTitle() + (end < content.length() ? "（续）" : ""));
                sub.setContent(content.substring(start, end).trim());
                sub.setStartOffset(u.getStartOffset() + start);
                sub.setEndOffset(u.getStartOffset() + end);
                sub.setKeywords(textUtil.keywords(sub.getContent(), 6));
                result.add(sub);
                if (end >= content.length()) {
                    break;
                }
                start += size;
            }
        }
        return result;
    }
}
