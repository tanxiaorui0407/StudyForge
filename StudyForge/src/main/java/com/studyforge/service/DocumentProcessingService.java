package com.studyforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.chunking.ChunkUnit;
import com.studyforge.chunking.ChunkingContext;
import com.studyforge.chunking.ChunkingEvaluator;
import com.studyforge.chunking.ChunkingStrategyRegistry;
import com.studyforge.chunking.ChunkingStrategyType;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.entity.Chunk;
import com.studyforge.entity.DocumentRecord;
import com.studyforge.entity.DocumentStatus;
import com.studyforge.entity.KnowledgeCard;
import com.studyforge.entity.Task;
import com.studyforge.entity.TaskStage;
import com.studyforge.entity.TaskStatus;
import com.studyforge.mapper.ChunkMapper;
import com.studyforge.mapper.DocumentMapper;
import com.studyforge.mapper.KnowledgeCardMapper;
import com.studyforge.mapper.TaskMapper;
import com.studyforge.parser.ParsedDocument;
import com.studyforge.parser.ParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档处理流水线（异步）：
 *   PARSE  -> 解析文本
 *   CHUNK  -> 智能分块（默认语义边界策略，可配置）
 *   OUTLINE-> Map-Reduce 生成大纲（含校验/重试/兜底）
 *   CARD   -> 生成知识卡片
 *   SUCCEEDED/FAILED
 * 每一步都更新 document.status 与 task 的 stage/progress，供前端轮询。
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final ParserRegistry parserRegistry;
    private final ChunkingStrategyRegistry chunkingRegistry;
    private final ChunkingEvaluator evaluator;
    private final OutlineService outlineService;
    private final CardService cardService;
    private final StudyForgeProperties props;
    private final ObjectMapper mapper;

    private final DocumentMapper documentMapper;
    private final TaskMapper taskMapper;
    private final ChunkMapper chunkMapper;
    private final KnowledgeCardMapper cardMapper;

    public DocumentProcessingService(ParserRegistry parserRegistry,
                                     ChunkingStrategyRegistry chunkingRegistry,
                                     ChunkingEvaluator evaluator,
                                     OutlineService outlineService,
                                     CardService cardService,
                                     StudyForgeProperties props,
                                     ObjectMapper mapper,
                                     DocumentMapper documentMapper,
                                     TaskMapper taskMapper,
                                     ChunkMapper chunkMapper,
                                     KnowledgeCardMapper cardMapper) {
        this.parserRegistry = parserRegistry;
        this.chunkingRegistry = chunkingRegistry;
        this.evaluator = evaluator;
        this.outlineService = outlineService;
        this.cardService = cardService;
        this.props = props;
        this.mapper = mapper;
        this.documentMapper = documentMapper;
        this.taskMapper = taskMapper;
        this.chunkMapper = chunkMapper;
        this.cardMapper = cardMapper;
    }

    /**
     * 在后台线程池中执行完整流水线。
     */
    public void process(Long documentId, byte[] bytes, String contentType) {
        DocumentRecord doc = documentMapper.selectById(documentId);
        Task task = latestTask(documentId);
        long start = System.currentTimeMillis();
        try {
            // 1) PARSE
            transition(doc, DocumentStatus.PARSING, task, TaskStage.PARSE, 10);
            ParsedDocument parsed = parserRegistry
                    .resolve(contentType, doc.getFileName())
                    .parse(bytes, doc.getFileName());
            doc.setFullText(parsed.getFullText());

            // 2) CHUNK
            transition(doc, DocumentStatus.CHUNKING, task, TaskStage.CHUNK, 30);
            ChunkingStrategyType strategy = ChunkingStrategyType.SEMANTIC_BOUNDARY;
            ChunkingContext ctx = ChunkingContext.fromProperties(
                    props.getChunking().getFixedSize(),
                    props.getChunking().getOverlap(),
                    props.getChunking().getMaxBlocks(),
                    props.getChunking().getMinBlockLen());
            List<ChunkUnit> units = chunkingRegistry.get(strategy).chunk(parsed, ctx);
            List<Chunk> persisted = persistChunks(documentId, strategy, units);
            log.info("分块完成：{} 块（策略 {}）", units.size(), strategy);

            // 3) OUTLINE（Map-Reduce / 兜底）
            transition(doc, DocumentStatus.OUTLINING, task, TaskStage.OUTLINE, 60);
            OutlineService.OutlineResult outline = outlineService.generateOutline(parsed, units);
            doc.setOutlineJson(mapper.writeValueAsString(outline.root));
            doc.setOutlineMode(outline.mode.name());
            doc.setTokenUsed((doc.getTokenUsed() == null ? 0 : doc.getTokenUsed()) + outline.tokenUsed);

            // 4) CARD
            transition(doc, DocumentStatus.CARDING, task, TaskStage.CARD, 80);
            List<KnowledgeCard> cards = cardService.generate(documentId, units, doc.getFileName(),
                    props.getChunking().getMaxBlocks() / 5);
            for (int i = 0; i < cards.size(); i++) {
                KnowledgeCard card = cards.get(i);
                if (!persisted.isEmpty()) {
                    int idx = Math.min(i, persisted.size() - 1);
                    card.setChunkId(persisted.get(idx).getId());
                }
                cardMapper.insert(card);
            }

            // 完成
            doc.setStatus(DocumentStatus.SUCCEEDED.name());
            doc.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(doc);

            task.setStage(TaskStage.CARD.name());
            task.setStatus(TaskStatus.SUCCEEDED.name());
            task.setProgress(100);
            task.setDurationMs(System.currentTimeMillis() - start);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            log.info("文档 {} 处理完成，耗时 {}ms，卡片 {} 张", documentId, task.getDurationMs(), cards.size());
        } catch (Exception e) {
            log.error("文档 {} 处理失败", documentId, e);
            doc.setStatus(DocumentStatus.FAILED.name());
            doc.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(doc);

            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage(truncate(e.getMessage(), 500));
            task.setDurationMs(System.currentTimeMillis() - start);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private void transition(DocumentRecord doc, DocumentStatus docStatus, Task task,
                            TaskStage stage, int progress) {
        doc.setStatus(docStatus.name());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        task.setStage(stage.name());
        task.setStatus(TaskStatus.RUNNING.name());
        task.setProgress(progress);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private List<Chunk> persistChunks(Long documentId, ChunkingStrategyType strategy, List<ChunkUnit> units) {
        List<Chunk> persisted = units.stream().map(u -> {
            Chunk c = new Chunk();
            c.setDocumentId(documentId);
            c.setChunkIndex(u.getIndex());
            c.setStrategy(strategy.name());
            c.setStartOffset(u.getStartOffset());
            c.setEndOffset(u.getEndOffset());
            c.setTitle(u.getTitle());
            c.setContent(u.getContent());
            c.setKeywords(u.getKeywords() == null ? "" : String.join(",", u.getKeywords()));
            return c;
        }).toList();
        for (Chunk c : persisted) {
            chunkMapper.insert(c);
        }
        return persisted;
    }

    private Task latestTask(Long documentId) {
        return taskMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Task>lambdaQuery()
                        .eq(Task::getDocumentId, documentId)
                        .orderByDesc(Task::getId)
                        .last("LIMIT 1")
        ).stream().findFirst().orElseGet(() -> {
            Task t = new Task();
            t.setDocumentId(documentId);
            t.setStage(TaskStage.PARSE.name());
            t.setStatus(TaskStatus.RUNNING.name());
            t.setProgress(0);
            return t;
        });
    }

    private String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() > n ? s.substring(0, n) : s;
    }
}
