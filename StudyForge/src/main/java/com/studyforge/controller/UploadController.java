package com.studyforge.controller;

import com.studyforge.dto.ApiResponse;
import com.studyforge.dto.UploadResponse;
import com.studyforge.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 上传接口：上传 PDF/MD/TXT，立即返回 taskId，后台异步处理。
 */
@RestController
@RequestMapping("/api/documents")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(uploadService.upload(file));
    }
}
