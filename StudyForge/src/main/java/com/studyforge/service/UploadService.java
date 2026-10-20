package com.studyforge.service;

import com.studyforge.config.StudyForgeProperties;
import com.studyforge.dto.UploadResponse;
import com.studyforge.entity.DocumentRecord;
import com.studyforge.entity.DocumentStatus;
import com.studyforge.entity.Task;
import com.studyforge.entity.TaskStage;
import com.studyforge.entity.TaskStatus;
import com.studyforge.mapper.DocumentMapper;
import com.studyforge.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 上传服务：
 *  1. 计算文件哈希；
 *  2. 命中缓存（相同文件已成功处理过）-> 直接复用，不重复烧 token；
 *  3. 否则落库 document + task，提交后台线程池处理，立即返回 taskId。
 * 全程本地处理、用户自备 API Key，文件不进任何三方存储。
 */
@Service
public class UploadService {

    private final CacheService cacheService;
    private final DocumentMapper documentMapper;
    private final TaskMapper taskMapper;
    private final DocumentProcessingService processor;

    public UploadService(CacheService cacheService, DocumentMapper documentMapper,
                         TaskMapper taskMapper, DocumentProcessingService processor) {
        this.cacheService = cacheService;
        this.documentMapper = documentMapper;
        this.taskMapper = taskMapper;
        this.processor = processor;
    }

    public UploadResponse upload(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名为空");
        }
        byte[] bytes = file.getBytes();
        String hash = md5(bytes);

        // 缓存复用：相同内容文件直接返回已有结果
        Optional<Long> reused = cacheService.findReusableDocumentId(hash);
        if (reused.isPresent()) {
            return new UploadResponse(null, reused.get(), "REUSED", true);
        }

        DocumentRecord doc = new DocumentRecord();
        doc.setFileName(fileName);
        doc.setFileHash(hash);
        doc.setSizeBytes((long) bytes.length);
        doc.setContentType(file.getContentType());
        doc.setStatus(DocumentStatus.PENDING.name());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);

        Task task = new Task();
        task.setDocumentId(doc.getId());
        task.setStage(TaskStage.PARSE.name());
        task.setStatus(TaskStatus.RUNNING.name());
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 异步处理：上传即返回 taskId
        processor.process(doc.getId(), bytes, file.getContentType());

        return new UploadResponse(task.getId(), doc.getId(), DocumentStatus.PENDING.name(), false);
    }

    public static String md5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }
}
