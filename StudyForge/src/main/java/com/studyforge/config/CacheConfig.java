package com.studyforge.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存。
 * 用途：文件哈希 -> 文档 id 的快速查重，避免重复解析相同文件、重复烧 token。
 */
@Configuration
public class CacheConfig {

    /** key: 文件 MD5；value: 已成功处理过的 document id（-1 表示从未见过） */
    @Bean
    public Cache<String, Long> fileHashCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(1))
                .build();
    }
}
