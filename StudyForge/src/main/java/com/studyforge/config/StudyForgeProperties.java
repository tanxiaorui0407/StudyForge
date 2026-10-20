package com.studyforge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * StudyForge 业务配置，绑定 application.yml 里的 studyforge.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "studyforge")
public class StudyForgeProperties {

    private Llm llm = new Llm();
    private Chunking chunking = new Chunking();
    private Outline outline = new Outline();
    private Cache cache = new Cache();

    /** LLM（OpenAI 兼容接口）配置 */
    @Data
    public static class Llm {
        private boolean enabled = false;                 // 是否启用 LLM
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private int maxTokens = 2048;
        private int timeoutSeconds = 60;
    }

    /** 分块参数 */
    @Data
    public static class Chunking {
        private int fixedSize = 1200;      // 固定长度策略每块字符数
        private int overlap = 120;         // 相邻块重叠字符数
        private int maxBlocks = 300;       // 单文档最大块数
        private int minBlockLen = 80;      // 过短块并入前一块
    }

    /** Map-Reduce 大纲参数 */
    @Data
    public static class Outline {
        private int mapReduceRetries = 2;  // JSON 校验失败后的重试次数
        private int maxOutlineNodes = 80;  // 大纲节点上限
    }

    /** 文件哈希缓存 */
    @Data
    public static class Cache {
        private boolean enabled = true;    // 相同文件复用结果，省 token
    }
}
