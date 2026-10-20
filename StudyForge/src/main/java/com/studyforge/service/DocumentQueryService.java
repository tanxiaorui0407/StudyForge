package com.studyforge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.chunking.ChunkEvalMetrics;
import com.studyforge.chunking.ChunkUnit;
import com.studyforge.chunking.ChunkingContext;
import com.studyforge.chunking.ChunkingEvaluator;
import com.studyforge.chunking.ChunkingStrategy;
import com.studyforge.chunking.ChunkingStrategyRegistry;
import com.studyforge.config.StudyForgeProperties;
import com.studyforge.dto.CardDto;
import com.studyforge.dto.ChunkDto;
import com.studyforge.dto.DocumentDetailResponse;
import com.studyforge.dto.DocumentSummaryResponse;
import com.studyforge.dto.OutlineNode;
import com.studyforge.dto.StrategyEvalResponse;
import com.studyforge.dto.TaskStatusResponse;
import com.studyforge.entity.Chunk;
import com.studyforge.entity.DocumentRecord;
import com.studyforge.entity.KnowledgeCard;
import com.studyforge.entity.Task;
import com.studyforge.mapper.ChunkMapper;
import com.studyforge.mapper.DocumentMapper;
import com.studyforge.mapper.KnowledgeCardMapper;
import com.studyforge.mapper.TaskMapper;
import com.studyforge.parser.HeadingExtractor;
import com.studyforge.parser.ParsedDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询服务：任务状态、文档列表/详情、分块策略对比、卡片、API 统计。
 */
@Service
public class DocumentQueryService {

    private final DocumentMapper documentMapper;
    private final TaskMapper taskMapper;
    private final ChunkMapper chunkMapper;
    private final KnowledgeCardMapper cardMapper;
    private final ChunkingStrategyRegistry chunkingRegistry;
    private final ChunkingEvaluator evaluator;
    private final StudyForgeProperties props;
    private final HeadingExtractor headingExtractor;
    private final ObjectMapper mapper;

    public DocumentQueryService(DocumentMapper documentMapper, TaskMapper taskMapper,
                                ChunkMapper chunkMapper, KnowledgeCardMapper cardMapper,
                                ChunkingStrategyRegistry chunkingRegistry, ChunkingEvaluator evaluator,
                                StudyForgeProperties props, HeadingExtractor headingExtractor, ObjectMapper mapper) {
        this.documentMapper = documentMapper;
        this.taskMapper = taskMapper;
        this.chunkMapper = chunkMapper;
        this.cardMapper = cardMapper;
        this.chunkingRegistry = chunkingRegistry;
        this.evaluator = evaluator;
        this.props = props;
        this.headingExtractor = headingExtractor;
        this.mapper = mapper;
    }

    // ---------- 任务 ----------

    public TaskStatusResponse getTaskStatus(Long taskId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return new TaskStatusResponse(t.getId(), t.getDocumentId(), t.getStage(),
                t.getStatus(), t.getProgress(), t.getErrorMessage());
    }

    // ---------- 文档 ----------

    public List<DocumentSummaryResponse> listDocuments() {
        List<DocumentRecord> docs = documentMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<DocumentRecord>lambdaQuery()
                        .orderByDesc(DocumentRecord::getId)
                        .last("LIMIT 100"));
        List<DocumentSummaryResponse> out = new ArrayList<>();
        for (DocumentRecord d : docs) {
            int chunkCount = Math.toIntExact(chunkMapper.selectCount(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<Chunk>lambdaQuery()
                            .eq(Chunk::getDocumentId, d.getId())));
            int cardCount = Math.toIntExact(cardMapper.selectCount(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<KnowledgeCard>lambdaQuery()
                            .eq(KnowledgeCard::getDocumentId, d.getId())));
            out.add(new DocumentSummaryResponse(d.getId(), d.getFileName(), d.getSizeBytes(),
                    d.getStatus(), d.getOutlineMode(), chunkCount, cardCount,
                    d.getTokenUsed() == null ? 0 : d.getTokenUsed()));
        }
        return out;
    }

    public DocumentDetailResponse getDocument(Long documentId) {
        DocumentRecord d = documentMapper.selectById(documentId);
        if (d == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }
        DocumentDetailResponse resp = new DocumentDetailResponse();
        resp.setId(d.getId());
        resp.setFileName(d.getFileName());
        resp.setStatus(d.getStatus());
        resp.setOutlineMode(d.getOutlineMode());
        resp.setCreatedAt(d.getCreatedAt() == null ? "" : d.getCreatedAt().toString());

        resp.setOutline(parseOutline(d.getOutlineJson()));

        List<Chunk> chunks = chunkMapper.listByDocument(documentId);
        resp.setChunks(chunks.stream().map(c -> new ChunkDto(c.getId(), c.getChunkIndex(),
                c.getTitle(), c.getKeywords(), c.getContent())).toList());

        List<KnowledgeCard> cards = cardMapper.listByDocument(documentId);
        resp.setCards(cards.stream().map(c -> new CardDto(c.getId(), c.getFront(), c.getBack(),
                c.getTags(), c.getSourceRef())).toList());
        return resp;
    }

    private OutlineNode parseOutline(String json) {
        if (json == null || json.isBlank()) {
            OutlineNode empty = new OutlineNode("尚无大纲");
            empty.setChildren(new ArrayList<>());
            return empty;
        }
        try {
            return mapper.readValue(json, OutlineNode.class);
        } catch (Exception e) {
            OutlineNode empty = new OutlineNode("大纲解析失败");
            empty.setChildren(new ArrayList<>());
            return empty;
        }
    }

    // ---------- 分块策略对比 ----------

    public List<StrategyEvalResponse> compareStrategies(Long documentId) {
        DocumentRecord d = documentMapper.selectById(documentId);
        if (d == null || d.getFullText() == null || d.getFullText().isBlank()) {
            throw new IllegalArgumentException("文档无全文，无法对比分块策略: " + documentId);
        }
        ParsedDocument parsed = headingExtractor.build(d.getFullText(), d.getFileName(), true);
        ChunkingContext ctx = ChunkingContext.fromProperties(
                props.getChunking().getFixedSize(),
                props.getChunking().getOverlap(),
                props.getChunking().getMaxBlocks(),
                props.getChunking().getMinBlockLen());

        List<StrategyEvalResponse> out = new ArrayList<>();
        for (ChunkingStrategy s : chunkingRegistry.all()) {
            List<ChunkUnit> units = s.chunk(parsed, ctx);
            ChunkEvalMetrics m = evaluator.evaluate(units, parsed);
            StrategyEvalResponse r = new StrategyEvalResponse();
            r.setStrategy(s.type().name());
            r.setMetrics(m);
            r.setSampleBlocks(Math.min(units.size(), 5));
            r.setChunkTitles(units.stream().limit(5).map(ChunkUnit::getTitle).toList());
            out.add(r);
        }
        return out;
    }

    // ---------- 卡片 / 全部分块 ----------

    public List<KnowledgeCard> getCards(Long documentId) {
        return cardMapper.listByDocument(documentId);
    }

    public List<Chunk> getChunks(Long documentId) {
        return chunkMapper.listByDocument(documentId);
    }
}
