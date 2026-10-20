package com.studyforge.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标题推断（启发式）：
 *  - Markdown：识别 "#"/"##"/"###" 前缀，级别由井号数量决定；
 *  - 普通文本：识别编号式标题（1. / 1.1 / 第X章 / 一、），或“短行且无句末标点”视为标题。
 * 该结果服务于“按标题层级分块”和“LLM 失败时的标题降级兜底大纲”。
 */
@Component
public class HeadingExtractor {

    private static final Pattern MARKDOWN_H = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^\\s*((?:\\d+\\.)+\\d*)\\s+(.+?)\\s*$");
    private static final Pattern CHAPTER = Pattern.compile("^\\s*(第[一二三四五六七八九十百千0-9]+[章节篇部分])\\s*[:：]?\\s*(.*)$");
    private static final Pattern CHINESE_NUM = Pattern.compile("^\\s*([一二三四五六七八九十]+、)\\s*(.+?)\\s*$");
    private static final Pattern TERMINAL_PUNCT = Pattern.compile("[。．.！？?！]$");
    private static final Pattern ALL_NUM = Pattern.compile("^[\\d.\\s]+$");

    private static final int MAX_HEADING_LEN = 60;      // 长于该长度不视为标题
    private static final int MAX_HEADINGS = 200;        // 防止误报过多

    /**
     * 由全文构造 ParsedDocument，含标题层级。
     *
     * @param markdown 是否为 Markdown（启用 # 前缀识别）
     */
    public ParsedDocument build(String fullText, String fileName, boolean markdown) {
        ParsedDocument doc = new ParsedDocument();
        doc.setFullText(fullText);

        List<ParsedDocument.Heading> raw = new ArrayList<>();
        String[] lines = fullText.split("\n", -1);
        int offset = 0;
        for (String line : lines) {
            int lineStart = offset;
            offset += line.length() + 1; // +1 换行符
            ParsedDocument.Heading h = tryParseHeading(line, markdown);
            if (h != null) {
                h.setStartOffset(lineStart);
                raw.add(h);
            }
        }

        // 计算每个标题的结束偏移：到下一个标题为止
        for (int i = 0; i < raw.size(); i++) {
            ParsedDocument.Heading cur = raw.get(i);
            int end = (i + 1 < raw.size()) ? raw.get(i + 1).getStartOffset() : fullText.length();
            cur.setEndOffset(end);
        }

        // 标题数量保护，并推断文档标题
        if (raw.size() > MAX_HEADINGS) {
            raw = raw.subList(0, MAX_HEADINGS);
        }
        doc.setHeadings(raw);
        doc.setTitle(inferTitle(raw, fileName));
        return doc;
    }

    private ParsedDocument.Heading tryParseHeading(String line, boolean markdown) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // 1) Markdown 井号标题
        if (markdown) {
            Matcher m = MARKDOWN_H.matcher(trimmed);
            if (m.matches()) {
                ParsedDocument.Heading h = new ParsedDocument.Heading();
                h.setTitle(m.group(2).trim());
                h.setLevel(m.group(1).length());
                return h;
            }
        }

        // 2) 编号式标题，如 "1.2 标题"、"第3章 标题"、"一、标题"
        Matcher num = NUMBERED.matcher(trimmed);
        if (num.matches()) {
            String code = num.group(1);
            String title = num.group(2).trim();
            if (title.length() <= MAX_HEADING_LEN) {
                ParsedDocument.Heading h = new ParsedDocument.Heading();
                h.setTitle(code + " " + title);
                h.setLevel(countLevel(code));
                return h;
            }
        }
        Matcher chapter = CHAPTER.matcher(trimmed);
        if (chapter.matches()) {
            String title = chapter.group(1) + (chapter.group(2).isEmpty() ? "" : " " + chapter.group(2).trim());
            if (title.length() <= MAX_HEADING_LEN) {
                ParsedDocument.Heading h = new ParsedDocument.Heading();
                h.setTitle(title);
                h.setLevel(1);
                return h;
            }
        }
        Matcher cn = CHINESE_NUM.matcher(trimmed);
        if (cn.matches()) {
            String title = cn.group(1) + " " + cn.group(2).trim();
            if (title.length() <= MAX_HEADING_LEN) {
                ParsedDocument.Heading h = new ParsedDocument.Heading();
                h.setTitle(title);
                h.setLevel(1);
                return h;
            }
        }

        // 3) 兜底启发式：短行、无句末标点、非纯数字 -> 视为标题
        if (!markdown
                && trimmed.length() <= MAX_HEADING_LEN
                && !ALL_NUM.matcher(trimmed).matches()
                && !TERMINAL_PUNCT.matcher(trimmed).find()) {
            ParsedDocument.Heading h = new ParsedDocument.Heading();
            h.setTitle(trimmed);
            h.setLevel(1);
            return h;
        }
        return null;
    }

    /** 由编号点个数推断层级：1 -> 1，1.1 -> 2，1.1.1 -> 3 */
    private int countLevel(String code) {
        int dots = 0;
        for (char c : code.toCharArray()) {
            if (c == '.') {
                dots++;
            }
        }
        return dots + 1;
    }

    private String inferTitle(List<ParsedDocument.Heading> headings, String fileName) {
        if (!headings.isEmpty() && headings.get(0).getLevel() == 1) {
            return headings.get(0).getTitle();
        }
        // 否则用文件名（去扩展名）
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
