package com.studyforge.service;

import com.studyforge.entity.KnowledgeCard;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anki 导出：把知识卡片导出为 Anki 可导入的 CSV（制表符分隔，编码 UTF-8 BOM）。
 * 导入步骤：Anki -> 文件 -> 导入 -> 选择 .txt/.csv -> 字段顺序 正面/背面/标签。
 */
@Service
public class AnkiService {

    /**
     * 生成 Anki 导入用的 CSV 文本。
     * 字段顺序固定为：正面(front) \t 背面(back) \t 标签(tags)。
     */
    public String buildCsv(List<KnowledgeCard> cards) {
        StringBuilder sb = new StringBuilder();
        for (KnowledgeCard c : cards) {
            sb.append(escape(c.getFront()))
                    .append('\t')
                    .append(escape(c.getBack()))
                    .append('\t')
                    .append(escape(c.getTags()))
                    .append('\n');
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        // 转义制表符/换行/引号，保证单行单卡片
        return s.replace("\\", "\\\\")
                .replace("\t", " ")
                .replace("\r", "")
                .replace("\n", "<br>");
    }
}
