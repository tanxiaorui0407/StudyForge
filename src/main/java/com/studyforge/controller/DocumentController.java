package com.studyforge.controller;

import com.studyforge.dto.ApiResponse;
import com.studyforge.dto.DocumentDetailResponse;
import com.studyforge.dto.DocumentSummaryResponse;
import com.studyforge.dto.StrategyEvalResponse;
import com.studyforge.dto.TaskStatusResponse;
import com.studyforge.entity.KnowledgeCard;
import com.studyforge.reliability.ApiLogService;
import com.studyforge.service.AnkiService;
import com.studyforge.service.DocumentQueryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 查询接口：任务状态、文档列表/详情、策略对比、卡片 Anki 导出、API 调用统计。
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentQueryService queryService;
    private final AnkiService ankiService;
    private final ApiLogService apiLogService;

    public DocumentController(DocumentQueryService queryService, AnkiService ankiService,
                              ApiLogService apiLogService) {
        this.queryService = queryService;
        this.ankiService = ankiService;
        this.apiLogService = apiLogService;
    }

    /** 任务状态（上传后轮询） */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskStatusResponse> task(@PathVariable Long taskId) {
        return ApiResponse.ok(queryService.getTaskStatus(taskId));
    }

    /** 文档列表 */
    @GetMapping("/documents")
    public ApiResponse<List<DocumentSummaryResponse>> documents() {
        return ApiResponse.ok(queryService.listDocuments());
    }

    /** 文档详情（导图 + 分块 + 卡片） */
    @GetMapping("/documents/{id}")
    public ApiResponse<DocumentDetailResponse> document(@PathVariable Long id) {
        return ApiResponse.ok(queryService.getDocument(id));
    }

    /** 分块策略对比（同一文档用三种策略各跑一遍，出指标） */
    @GetMapping("/documents/{id}/compare")
    public ApiResponse<List<StrategyEvalResponse>> compare(@PathVariable Long id) {
        return ApiResponse.ok(queryService.compareStrategies(id));
    }

    /** 导出 Anki 卡片（CSV） */
    @GetMapping("/documents/{id}/cards/export")
    public ResponseEntity<byte[]> exportCards(@PathVariable Long id) {
        List<KnowledgeCard> cards = queryService.getCards(id);
        String csv = ankiService.buildCsv(cards);
        // 加 UTF-8 BOM，Anki/Excel 正确识别中文
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=studyforge_cards.txt")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    /** API 调用统计：总调用、token、失败数 */
    @GetMapping("/stats/llm")
    public ApiResponse<Map<String, Long>> llmStats() {
        ApiLogService.Summary s = apiLogService.summarize();
        return ApiResponse.ok(Map.of(
                "totalCalls", s.totalCalls,
                "totalTokens", s.totalTokens,
                "failures", s.failures));
    }
}
