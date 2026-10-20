package com.studyforge.reliability;

import com.studyforge.dto.OutlineNode;
import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标题降级兜底：当 LLM 不可用或大纲校验多次失败时，
 * 用解析阶段推断出的标题层级构建大纲（0 token 成本、稳定可用）。
 * 这是“可靠性套件”的最后一道防线，保证系统没有 API Key 也能完整跑通。
 */
@Component
public class FallbackOutlineGenerator {

    /**
     * 由标题层级构建树状大纲。
     * 无标题时，退化为“根 -> 各分块主题”的扁平结构。
     */
    public OutlineNode generate(ParsedDocument doc, List<?> chunkTitles) {
        OutlineNode root = new OutlineNode(doc.getTitle() == null ? "资料大纲" : doc.getTitle());
        root.setChildren(new ArrayList<>());

        List<ParsedDocument.Heading> headings = doc.getHeadings();
        if (headings == null || headings.isEmpty()) {
            for (Object t : chunkTitles) {
                OutlineNode leaf = new OutlineNode(String.valueOf(t));
                leaf.setChildren(new ArrayList<>());
                root.getChildren().add(leaf);
            }
            return root;
        }

        // lastAtLevel.get(level) = 该层级最近一个节点；挂子节点时找层级更小的最近父级
        Map<Integer, OutlineNode> lastAtLevel = new HashMap<>();
        for (ParsedDocument.Heading h : headings) {
            // 若该标题即文档根（首章），把它登记为 level1 容器，避免根自挂成自己子节点
            if (h.getLevel() == 1 && h.getTitle().equals(root.getTitle())) {
                lastAtLevel.put(1, root);
                continue;
            }
            OutlineNode node = new OutlineNode(h.getTitle());
            node.setChildren(new ArrayList<>());

            int parentLevel = h.getLevel() - 1;
            OutlineNode parent = root;
            for (int lv = parentLevel; lv >= 1; lv--) {
                OutlineNode candidate = lastAtLevel.get(lv);
                if (candidate != null) {
                    parent = candidate;
                    break;
                }
            }
            parent.getChildren().add(node);

            // 当前层级及其更深层级的旧记录作废
            lastAtLevel.entrySet().removeIf(e -> e.getKey() >= h.getLevel());
            lastAtLevel.put(h.getLevel(), node);
        }
        return root;
    }
}
