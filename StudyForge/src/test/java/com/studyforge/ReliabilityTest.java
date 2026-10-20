package com.studyforge;

import com.studyforge.dto.OutlineNode;
import com.studyforge.reliability.FallbackOutlineGenerator;
import com.studyforge.reliability.OutlineSchemaValidator;
import com.studyforge.parser.HeadingExtractor;
import com.studyforge.parser.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReliabilityTest {

    private final OutlineSchemaValidator validator = new OutlineSchemaValidator();
    private final FallbackOutlineGenerator fallback = new FallbackOutlineGenerator();

    @Test
    void schemaValidatorAcceptsWellFormedOutline() {
        OutlineNode root = new OutlineNode("资料");
        OutlineNode a = new OutlineNode("第一章");
        OutlineNode b = new OutlineNode("1.1 小节");
        b.setChildren(List.of(new OutlineNode("知识点")));
        a.setChildren(List.of(b));
        root.setChildren(List.of(a));

        OutlineSchemaValidator.ValidationResult r = validator.validate(root, 100);
        assertThat(r.valid).isTrue();
    }

    @Test
    void schemaValidatorRejectsBlankTitle() {
        OutlineNode root = new OutlineNode("");
        OutlineSchemaValidator.ValidationResult r = validator.validate(root, 100);
        assertThat(r.valid).isFalse();
        assertThat(r.errors).isNotEmpty();
    }

    @Test
    void schemaValidatorRejectsTooManyNodes() {
        OutlineNode root = new OutlineNode("根");
        OutlineNode[] kids = new OutlineNode[10];
        OutlineNode[] grand = new OutlineNode[10];
        OutlineNode[] great = new OutlineNode[10];
        for (int i = 0; i < 10; i++) kids[i] = new OutlineNode("a" + i);
        for (int i = 0; i < 10; i++) grand[i] = new OutlineNode("b" + i);
        for (int i = 0; i < 10; i++) great[i] = new OutlineNode("c" + i);
        root.setChildren(List.of(kids));
        for (int i = 0; i < 10; i++) kids[i].setChildren(List.of(grand[i]));
        for (int i = 0; i < 10; i++) grand[i].setChildren(List.of(great[i]));
        OutlineSchemaValidator.ValidationResult r = validator.validate(root, 20);
        assertThat(r.valid).isFalse();
    }

    @Test
    void fallbackBuildsNestedOutlineFromHeadings() {
        String md = "# 第一章\n## 1.1 小节\n## 1.2 小节\n# 第二章";
        ParsedDocument doc = new HeadingExtractor().build(md, "x.md", true);
        OutlineNode root = fallback.generate(doc, List.of("块1", "块2"));
        assertThat(root.getTitle()).isEqualTo("第一章");
        // 根标题即文档根，不重复挂；1.1/1.2 直接挂在根下，第二章是独立一级
        assertThat(root.getChildren()).extracting(OutlineNode::getTitle)
                .containsExactly("1.1 小节", "1.2 小节", "第二章");
    }

    @Test
    void fallbackProducesFlatLeavesWhenNoHeading() {
        ParsedDocument doc = new HeadingExtractor().build("没有任何标题的正文内容。", "x.txt", false);
        OutlineNode root = fallback.generate(doc, List.of("主题块 1", "主题块 2"));
        assertThat(root.getChildren()).hasSize(2);
    }
}
