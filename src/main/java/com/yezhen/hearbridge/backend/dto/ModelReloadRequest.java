package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Python 模型重载请求。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelReloadRequest {

    /**
     * 模型文件路径。
     */
    private String modelPath;

    /**
     * 标签映射文件路径。
     */
    private String labelMapPath;

    /**
     * 模型版本名称。
     */
    private String versionName;
}
