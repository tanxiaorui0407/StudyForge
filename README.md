# StudyForge · 复习资料拆解工作台

> 上传 PDF / MD / TXT → 智能分块 → 层级思维导图 + 知识卡片 → 导出 Anki 复习。
> 为“几十页 PDF 背不完、不知道哪里是重点”的求职复习场景，构建 **读 → 拆 → 记** 的闭环。

与 TreeMind 等导图工具的差异：**不止给导图**，还按块生成带面试问法的知识卡片，并可一键导出 Anki，形成可循环复习的工作台。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3（Java 17） |
| 持久层 | MyBatis-Plus + MySQL（测试/CI 用 H2，无需数据库也能跑构建） |
| 缓存 | Caffeine（相同文件哈希复用，省 token） |
| PDF 解析 | Apache PDFBox |
| 前端渲染导图 | markmap.js（现成库，前端由 Spring 托管在 `static/`） |
| 测试 | JUnit5 + AssertJ |
| CI | GitHub Actions（`mvn verify`） |
| 部署 | Docker + docker-compose（MySQL + 应用） |

## 二、四个核心设计

### 1. 分块策略可插拔
`chunking/ChunkingStrategy` 是接口，三种实现即插即用：
- `FixedLengthStrategy` — 固定长度 + 重叠窗口（确定性、最稳定）；
- `HeadingBasedStrategy` — 按标题层级切分（块标题 = 章节名，可解释性最强）；
- `SemanticBoundaryStrategy` — 语义边界 / 话题转移检测（相邻句窗口词汇重叠度骤降即切块，0 token 成本）。

配套 **评估器** `ChunkingEvaluator`：算“**块内主题一致性**”（块内相邻句词汇重叠度均值）等指标，前端“分块策略对比”页用数据对比三种策略，不拍脑袋。

> 升级路径（README 里讲清）：把词汇重叠度换成 embedding 向量余弦相似度，即成为真正的向量级语义分块——分块是可插拔的，换实现即可。

### 2. Map-Reduce 式大纲生成
`service/OutlineService`：
- 长文档塞不进一次 LLM 上下文 → **MAP**：逐块让 LLM 提炼小结+关键词；
- **REDUCE**：合并所有小结，让 LLM 生成层级大纲（JSON）；
- 这也顺便控制了 token 成本与 context 溢出。

### 3. 可靠性套件（`reliability/`）
- **JSON Schema 校验** `OutlineSchemaValidator`：校验大纲结构（非空标题、层级上限、节点数上限）；
- **失败重试** `RetryRunner`：指数退避；
- **降级兜底** `FallbackOutlineGenerator`：LLM 失败/未配置 → 纯标题层级解析，0 token 也能完整跑通；
- **相同文件哈希缓存** `CacheService`：MD5 命中已成功文档 → 直接复用，不重复烧 token（Caffeine + MySQL 两级）；
- **可观测**：每次 LLM 调用落 `api_call_log`（token / 耗时 / 失败原因），接口 `/api/stats/llm` 可查总成本。

### 4. 异步任务（上传即返回 taskId）
大文件解析耗时 → `task` 表 + 状态轮询。上传接口立即返回 `taskId`，前端轮询进度（解析→分块→大纲→卡片各阶段进度条）。

## 三、隐私与成本
- **本地处理**：文件只在本机内存里解析，不落三方存储、不上传任何外部服务；
- **自备 API Key**：不配 Key 也能用（自动降级标题兜底大纲 + 模板卡片），配了 Key 才调 LLM；
- 相同文件重复上传走缓存，不重复烧 token。

---

## 四、本地运行

### 方式 A：Docker 一键（推荐，免装环境）
```bash
# 1) 复制并填写你的 API Key（可选）
cp .env.example .env      # 没有该文件就手动建：LLM_API_KEY=sk-xxx

# 2) 启动 MySQL + 应用
docker compose up -d --build

# 3) 打开前端
# http://localhost:8080
```

### 方式 B：本机开发（需 JDK17 + Maven + MySQL）
```bash
# 1) 建库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS studyforge DEFAULT CHARACTER SET utf8mb4;"

# 2) 改连接（默认 root 场景可改环境变量）
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/studyforge?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=你的密码

# 3) 配置 LLM（可选）
export LLM_ENABLED=true
export LLM_API_KEY=sk-xxx        # DeepSeek / OpenAI 兼容都行
export LLM_BASE_URL=https://api.deepseek.com
export LLM_MODEL=deepseek-chat

# 4) 跑起来
mvn spring-boot:run
```

> 想不装 MySQL 先看效果：`mvn spring-boot:run -Dspring.profiles.active=test`（用 H2，数据不持久）。

## 五、测试与 CI
```bash
mvn clean verify            # 编译 + 单测 + Spring 上下文冒烟（自动用 H2，无需 MySQL）
```
推送到 GitHub 后，`.github/workflows/ci.yml` 会自动跑同样的构建。

## 六、接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/documents/upload` | 上传 PDF/MD/TXT，返回 `taskId` |
| GET | `/api/tasks/{taskId}` | 轮询任务状态/进度 |
| GET | `/api/documents` | 文档列表 |
| GET | `/api/documents/{id}` | 详情（导图 + 分块 + 卡片） |
| GET | `/api/documents/{id}/compare` | 三种分块策略指标对比 |
| GET | `/api/documents/{id}/cards/export` | 导出 Anki（CSV） |
| GET | `/api/stats/llm` | LLM 调用统计（token/失败） |

## 七、目录速览
```
src/main/java/com/studyforge/
  chunking/     # 分块策略 + 评估器（核心亮点1）
  service/      # OutlineService(Map-Reduce)、CardService、AnkiService、异步流水线
  reliability/  # JSON校验/重试/标题兜底/哈希缓存/调用日志（核心亮点3）
  llm/          # OpenAI 兼容客户端
  parser/       # PDF/MD/TXT 解析 + 标题推断
  entity/ mapper/ dto/ controller/ config/
src/main/resources/
  static/index.html   # markmap.js 前端
  schema.sql          # 建表 SQL
  application.yml
```

