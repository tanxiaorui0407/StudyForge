package com.studyforge.chunking;

import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本工具：中文二元组分词、关键词提取、词汇重叠度（供语义分块与主题一致性评估复用）。
 * 说明：不引入外部中文分词依赖，用轻量的字符二元组（CJK）+ 英文单词作为 token，
 * 足以支撑“话题转移检测”与“块内主题一致性”指标。
 */
@Component
public class TextUtil {

    private static final java.util.Set<String> STOPWORDS = java.util.Set.of(
            "的", "了", "在", "是", "和", "与", "及", "或", "等", "对", "中", "上", "下",
            "我们", "你们", "他们", "这", "那", "之", "一个", "进行", "可以", "没有", "不是",
            "the", "a", "an", "is", "are", "of", "to", "and", "in", "on", "for", "with", "as", "by", "at", "that", "this"
    );

    /**
     * 把一段文本切成 token 列表：
     *  - 英文/数字按空白切分（保留原始词，如 TCP -> "tcp"）；
     *  - 中文连续串按相邻字符二元组切分（兼顾分词与主题一致性度量）。
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        StringBuilder ascii = new StringBuilder();
        StringBuilder cjk = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (Character.isIdeographic(c)) {
                flushAscii(ascii, tokens); // 中文开始，先结算英文词
                cjk.append(c);
            } else if (Character.isLetterOrDigit(c)) {
                flushCjk(cjk, tokens);     // 英文/数字开始，先结算中文
                ascii.append(c);
            } else {
                flushAscii(ascii, tokens);
                flushCjk(cjk, tokens);
            }
            i++;
        }
        flushAscii(ascii, tokens);
        flushCjk(cjk, tokens);
        return tokens;
    }

    private void flushAscii(StringBuilder ascii, List<String> out) {
        if (ascii.length() == 0) {
            return;
        }
        String w = ascii.toString().toLowerCase();
        if (w.length() >= 2) {
            out.add(w);
        }
        ascii.setLength(0);
    }

    private void flushCjk(StringBuilder cjk, List<String> out) {
        if (cjk.length() == 0) {
            return;
        }
        String seg = cjk.toString();
        if (seg.length() == 1) {
            out.add(seg);
        } else {
            for (int k = 0; k + 1 < seg.length(); k++) {
                out.add(seg.substring(k, k + 2));
            }
        }
        cjk.setLength(0);
    }

    /** 词频统计，返回按频次降序的 {词: 次数} */
    public Map<String, Integer> frequency(List<String> tokens) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String t : tokens) {
            if (t.length() < 2 || STOPWORDS.contains(t)) {
                continue; // 过滤过短与停用词
            }
            freq.merge(t, 1, Integer::sum);
        }
        return freq;
    }

    /** 提取前 topK 个高频词作为关键词 */
    public List<String> keywords(String text, int topK) {
        List<String> tokens = tokenize(text);
        Map<String, Integer> freq = frequency(tokens);
        return freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * 两个 token 集合的词汇重叠度（Jaccard）：
     * 1 表示高度一致，0 表示完全不同。用于话题转移检测与主题一致性指标。
     */
    public double vocabOverlap(List<String> a, List<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        java.util.Set<String> sa = new java.util.HashSet<>(a);
        java.util.Set<String> sb = new java.util.HashSet<>(b);
        java.util.Set<String> inter = new java.util.HashSet<>(sa);
        inter.retainAll(sb);
        java.util.Set<String> union = new java.util.HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0 : (double) inter.size() / union.size();
    }

    /** 把句子切出来（保留顺序），用于语义分块 */
    public List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return sentences;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<=[。！？!?\\n])");
        String[] parts = p.split(text);
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) {
                sentences.add(t);
            }
        }
        return sentences;
    }
}
