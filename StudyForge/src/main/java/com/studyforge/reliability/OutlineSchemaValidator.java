package com.studyforge.reliability;

import com.studyforge.dto.OutlineNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量 JSON Schema 校验：校验 LLM 返回的大纲结构。
 * 规则（等价于一个 outline.schema.json）：
 *   - 节点必须有非空 title（字符串）；
 *   - children 若存在必须是数组，且每个元素递归满足本规则；
 *   - 深度不超过 8 层、节点总数不超过配置上限——防止 LLM 输出失控。
 * 校验失败 -> 触发重试；重试仍失败 -> 降级为标题兜底。
 */
@Component
public class OutlineSchemaValidator {

    private static final int MAX_DEPTH = 8;

    public ValidationResult validate(OutlineNode root, int maxNodes) {
        List<String> errors = new ArrayList<>();
        int[] count = {0};
        validateNode(root, 0, maxNodes, count, errors);
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateNode(OutlineNode node, int depth, int maxNodes, int[] count, List<String> errors) {
        if (node == null) {
            errors.add("节点为 null");
            return;
        }
        count[0]++;
        if (count[0] > maxNodes) {
            errors.add("节点总数超过上限 " + maxNodes);
            return;
        }
        if (node.getTitle() == null || node.getTitle().isBlank()) {
            errors.add("存在空标题节点");
        }
        if (depth > MAX_DEPTH) {
            errors.add("大纲层级超过 " + MAX_DEPTH);
            return;
        }
        if (node.getChildren() != null) {
            for (OutlineNode c : node.getChildren()) {
                validateNode(c, depth + 1, maxNodes, count, errors);
            }
        }
    }

    public static class ValidationResult {
        public final boolean valid;
        public final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
    }
}
