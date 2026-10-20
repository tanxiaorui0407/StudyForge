package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略三：语义边界（话题转移检测）。
 * 在相邻句子窗口之间计算词汇重叠度，重叠度骤降处即话题转移，作为块边界。
 * 这是对 embedding 向量相似度的一种轻量替代：0 token 成本、确定性可复现，
 * 文档里会解释如何用 embedding 升级成真正的语义向量分块。
 */
@Component
public class SemanticBoundaryStrategy implements ChunkingStrategy {

    private static final int WINDOW = 3;          // 滑动窗口句子数
    private static final double BOUNDARY_THRESHOLD = 0.15; // 重叠度低于此值视为话题转移
    private static final int SENT_MIN = 4;        // 块内最少句子数（避免切太碎）
    private static final int SENT_MAX = 40;       // 块内最多句子数

    private final TextUtil textUtil;
    private final FixedLengthStrategy fallback;

    public SemanticBoundaryStrategy(TextUtil textUtil, FixedLengthStrategy fallback) {
        this.textUtil = textUtil;
        this.fallback = fallback;
    }

    @Override
    public ChunkingStrategyType type() {
        return ChunkingStrategyType.SEMANTIC_BOUNDARY;
    }

    @Override
    public List<ChunkUnit> chunk(ParsedDocument doc, ChunkingContext ctx) {
        String text = doc.getFullText();
        List<String> sentences = textUtil.splitSentences(text);
        if (sentences.size() < SENT_MIN * 2) {
            // 句子太少，直接按固定长度兜底
            return fallbackFixed(text, doc, ctx);
        }

        List<ChunkUnit> units = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int curLen = 0;
        for (int i = 0; i < sentences.size(); i++) {
            current.add(sentences.get(i));
            curLen += sentences.get(i).length();
            boolean last = (i == sentences.size() - 1);
            // 达到句子上限或长度上限，必须切
            boolean hardCut = current.size() >= SENT_MAX
                    || (curLen >= ctx.getFixedSize() && current.size() >= SENT_MIN);
            boolean softCut = !last
                    && current.size() >= SENT_MIN
                    && isBoundary(sentences, i);
            if (hardCut || softCut) {
                units.add(buildUnit(units.size(), current, text));
                current.clear();
                curLen = 0;
            }
        }
        if (!current.isEmpty()) {
            units.add(buildUnit(units.size(), current, text));
        }
        return units;
    }

    /** 判断第 idx 个句子处是否话题转移：前后窗口词汇重叠度低 */
    private boolean isBoundary(List<String> sentences, int idx) {
        List<String> prev = window(sentences, idx - WINDOW, idx);
        List<String> next = window(sentences, idx, idx + WINDOW);
        if (prev.size() < 2 || next.size() < 2) {
            return false;
        }
        List<String> prevTokens = tokensOf(prev);
        List<String> nextTokens = tokensOf(next);
        double overlap = textUtil.vocabOverlap(prevTokens, nextTokens);
        return overlap < BOUNDARY_THRESHOLD;
    }

    private List<String> window(List<String> sentences, int from, int to) {
        List<String> w = new ArrayList<>();
        for (int i = Math.max(0, from); i < Math.min(sentences.size(), to); i++) {
            w.add(sentences.get(i));
        }
        return w;
    }

    private List<String> tokensOf(List<String> sentences) {
        List<String> all = new ArrayList<>();
        for (String s : sentences) {
            all.addAll(textUtil.tokenize(s));
        }
        return all;
    }

    private ChunkUnit buildUnit(int index, List<String> sentences, String fullText) {
        String content = String.join(" ", sentences);
        int start = fullText.indexOf(content);
        int end = start + content.length();
        ChunkUnit u = new ChunkUnit();
        u.setIndex(index);
        u.setContent(content.trim());
        u.setTitle("主题块 " + (index + 1));
        u.setStartOffset(Math.max(0, start));
        u.setEndOffset(Math.min(fullText.length(), end));
        u.setKeywords(textUtil.keywords(content, 6));
        return u;
    }

    private List<ChunkUnit> fallbackFixed(String text, ParsedDocument doc, ChunkingContext ctx) {
        return fallback.chunk(doc, ctx);
    }
}
