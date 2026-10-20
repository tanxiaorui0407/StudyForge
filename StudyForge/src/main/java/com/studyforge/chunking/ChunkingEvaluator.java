package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分块评估器：计算“块内主题一致性”等指标，用于对比三种策略。
 *
 * 主题一致性定义：把块再切成句子，相邻句窗口的词汇重叠度越高，说明块内话题越聚焦。
 * 数值越大越好（越一致）；通常与块数/块长配合看——一致性好且块数不过多的策略更优。
 */
@Component
public class ChunkingEvaluator {

    private final TextUtil textUtil;

    public ChunkingEvaluator(TextUtil textUtil) {
        this.textUtil = textUtil;
    }

    public ChunkEvalMetrics evaluate(List<ChunkUnit> units, ParsedDocument doc) {
        ChunkEvalMetrics m = new ChunkEvalMetrics();
        m.setBlockCount(units.size());
        m.setTotalChars(doc.getFullText().length());

        double sum = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (ChunkUnit u : units) {
            int len = u.getContent().length();
            sum += len;
            min = Math.min(min, len);
            max = Math.max(max, len);
        }
        int n = Math.max(1, units.size());
        double avg = sum / n;
        m.setAvgBlockLen(avg);
        m.setMinBlockLen((int) (n == units.size() && min != Long.MAX_VALUE ? min : avg));
        m.setMaxBlockLen((int) max);

        // 方差
        double var = 0;
        for (ChunkUnit u : units) {
            double d = u.getContent().length() - avg;
            var += d * d;
        }
        m.setSizeVariance(n > 1 ? var / (n - 1) : 0);

        // 块内主题一致性
        double totalConsistency = 0;
        int consistentBlocks = 0;
        for (ChunkUnit u : units) {
            double c = blockConsistency(u.getContent());
            if (c >= 0) {
                totalConsistency += c;
                consistentBlocks++;
            }
        }
        m.setAvgTopicConsistency(consistentBlocks > 0 ? totalConsistency / consistentBlocks : 0);

        // 覆盖率
        long covered = 0;
        for (ChunkUnit u : units) {
            covered += u.getContent().length();
        }
        m.setCoverageRatio(doc.getFullText().isEmpty() ? 0 : Math.min(1.0, (double) covered / doc.getFullText().length()));
        return m;
    }

    /** 块内相邻句窗口的词汇重叠度均值；句子太少返回 -1（表示不可评估） */
    private double blockConsistency(String content) {
        List<String> sentences = textUtil.splitSentences(content);
        if (sentences.size() < 2) {
            return -1;
        }
        double sum = 0;
        int pairs = 0;
        for (int i = 0; i + 1 < sentences.size(); i++) {
            List<String> a = textUtil.tokenize(sentences.get(i));
            List<String> b = textUtil.tokenize(sentences.get(i + 1));
            sum += textUtil.vocabOverlap(a, b);
            pairs++;
        }
        return pairs > 0 ? sum / pairs : -1;
    }
}
