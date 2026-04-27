package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Python 从 URL 重载模型请求。
 *
 * 用于 MinIO 化后的模型发布流程：
 * 1. Spring Boot 根据模型版本生成 MinIO 访问 URL；
 * 2. Python 下载模型和 label_map 到 runtime 缓存；
 * 3. Python 加载该模型作为当前实时识别模型。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelReloadFromUrlRequest {

    /**
     * 模型文件 URL。
     */
    private String modelUrl;

    /**
     * 标签映射文件 URL。
     */
    private String labelMapUrl;

    /**
     * 模型版本名称。
     */
    private String versionName;
}
