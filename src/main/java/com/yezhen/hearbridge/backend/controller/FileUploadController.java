package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.FileUploadResult;
import com.yezhen.hearbridge.backend.service.MinioStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传 Controller。
 *
 * 当前阶段用于管理端上传：
 * 1. 手势分类封面；
 * 2. 手势资源封面；
 * 3. 手势资源 SiGML 文件。
 */
@RestController
@RequestMapping("/files")
public class FileUploadController {

    /**
     * MinIO 文件存储服务。
     */
    private final MinioStorageService minioStorageService;

    /**
     * 构造注入 MinIO 文件存储服务。
     *
     * @param minioStorageService MinIO 文件存储服务
     */
    public FileUploadController(MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    /**
     * 上传文件。
     *
     * 请求类型：
     * multipart/form-data
     *
     * 参数：
     * 1. file：文件；
     * 2. bizType：业务类型。
     *
     * @param file    上传文件
     * @param bizType 业务类型
     * @return 上传结果
     */
    @PostMapping("/upload")
    public FileUploadResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType) {
        return minioStorageService.uploadByBizType(file, bizType);
    }
}
