package com.studyforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring 上下文冒烟测试：验证所有 Bean 装配正确、H2 schema 能初始化。
 * 使用 test profile（H2，无需 MySQL）。
 */
@SpringBootTest(properties = "spring.profiles.active=test")
class StudyForgeApplicationTests {

    @Test
    void contextLoads() {
        // 只要能装配起来即通过
    }
}
