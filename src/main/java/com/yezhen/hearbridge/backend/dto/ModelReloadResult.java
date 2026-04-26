package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Python 模型重载结果。
 */
@Getter
@Setter
public class ModelReloadResult {

    /**
     * 是否重载成功。
     */
    private Boolean ok;

    /**
     * 模型版本名称。
     */
    private String versionName;

    /**
     * 模型文件路径。
     */
    private String modelPath;

    /**
     * 标签映射文件路径。
     */
    private String labelMapPath;

    /**
     * 类别数量。
     */
    private Integer classCount;

    /**
     * 结果说明。
     */
    private String message;
}
