package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文件上传结果。
 *
 * 用于返回给前端：
 * 1. objectKey：数据库建议保存的 MinIO 对象 Key；
 * 2. url：前端 / 手机端可直接访问的完整资源地址。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {

    /**
     * MinIO 桶名称。
     */
    private String bucket;

    /**
     * MinIO 对象 Key。
     *
     * 数据库中建议保存这个字段，而不是保存完整 URL。
     */
    private String objectKey;

    /**
     * 浏览器 / 手机端可访问的完整 URL。
     */
    private String url;

    /**
     * 原始文件名。
     */
    private String originalFileName;

    /**
     * 归一化后的文件类型。
     */
    private String contentType;

    /**
     * 文件大小，单位 byte。
     */
    private Long size;
}
