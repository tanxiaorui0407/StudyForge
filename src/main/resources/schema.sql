-- StudyForge 表结构（兼容 MySQL 与 H2 测试库）
-- Spring Boot 启动时会自动执行本脚本（spring.sql.init.mode=always）

CREATE TABLE IF NOT EXISTS `document` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name     VARCHAR(255) NOT NULL,
  file_hash     CHAR(32)     NOT NULL,          -- 文件内容 MD5
  size_bytes    BIGINT       NOT NULL DEFAULT 0,
  content_type  VARCHAR(64),
  status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING', -- PENDING/PARSING/CHUNKING/OUTLINING/CARDING/SUCCEEDED/FAILED
  outline_json  TEXT,                            -- 生成的层级大纲（JSON）
  outline_mode  VARCHAR(32),                     -- LLM_MAP_REDUCE / FALLBACK_HEADING
  full_text     TEXT,                            -- 解析后的全文（用于策略对比/卡片回溯）
  token_used    BIGINT       NOT NULL DEFAULT 0, -- 该文档累计消耗 token
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_document_hash ON `document` (file_hash);

CREATE TABLE IF NOT EXISTS `chunk` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id   BIGINT       NOT NULL,
  chunk_index   INT          NOT NULL,
  strategy      VARCHAR(32)  NOT NULL,   -- FIXED_LENGTH / HEADING_BASED / SEMANTIC_BOUNDARY
  start_offset  INT,
  end_offset    INT,
  title         VARCHAR(512),
  content       TEXT         NOT NULL,
  keywords      VARCHAR(1024),           -- 块内关键词（主题一致性的直观体现）
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chunk_doc ON `chunk` (document_id);

CREATE TABLE IF NOT EXISTS `task` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id   BIGINT       NOT NULL,
  stage         VARCHAR(32)  NOT NULL,   -- PARSE / CHUNK / OUTLINE / CARD
  status        VARCHAR(32)  NOT NULL DEFAULT 'RUNNING', -- RUNNING/SUCCEEDED/FAILED
  progress      INT          NOT NULL DEFAULT 0,
  error_message VARCHAR(1024),
  token_used    BIGINT       NOT NULL DEFAULT 0,
  duration_ms   BIGINT,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_doc ON `task` (document_id);

CREATE TABLE IF NOT EXISTS `knowledge_card` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT       NOT NULL,
  chunk_id    BIGINT,
  front       TEXT         NOT NULL,     -- 问题面（Anki 正面）
  back        TEXT         NOT NULL,     -- 答案面（Anki 背面）
  tags        VARCHAR(512),
  source_ref  VARCHAR(512),              -- 来源：第几块 / 原文位置，方便回溯
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_card_doc ON `knowledge_card` (document_id);

CREATE TABLE IF NOT EXISTS `api_call_log` (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  operation      VARCHAR(64)  NOT NULL,  -- CHUNK_MAP / OUTLINE_REDUCE / CARD_GEN ...
  model          VARCHAR(64),
  input_tokens   INT,
  output_tokens  INT,
  latency_ms     BIGINT,
  success        TINYINT      NOT NULL DEFAULT 1,
  error_type     VARCHAR(64),
  error_message  VARCHAR(1024),
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_apilog_op ON `api_call_log` (operation);
