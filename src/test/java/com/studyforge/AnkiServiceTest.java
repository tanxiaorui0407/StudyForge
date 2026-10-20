package com.studyforge;

import com.studyforge.entity.KnowledgeCard;
import com.studyforge.service.AnkiService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnkiServiceTest {

    private final AnkiService anki = new AnkiService();

    @Test
    void buildsTabSeparatedCsv() {
        KnowledgeCard c = new KnowledgeCard();
        c.setFront("问题？");
        c.setBack("答案\n带换行");
        c.setTags("tag1 tag2");

        String csv = anki.buildCsv(List.of(c));
        assertThat(csv).contains("\t");
        // 换行被转义为 <br>，保证单行单卡片
        assertThat(csv).doesNotContain("答案\n带");
        assertThat(csv).contains("答案<br>带换行");
        assertThat(csv).contains("tag1 tag2");
    }
}
