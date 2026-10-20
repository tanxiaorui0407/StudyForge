package com.studyforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * StudyForge 入口。
 * 技术栈：Spring Boot 3 + MyBatis-Plus + MySQL + Caffeine + PDFBox + markmap.js
 */
@SpringBootApplication
@MapperScan("com.studyforge.mapper")
@EnableAsync
public class StudyForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyForgeApplication.class, args);
    }
}
