package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * raw → feature 转换结果。
 */
@Getter
@Setter
public class FeatureConvertResult {

    /**
     * raw dataset 根目录。
     */
    private String rawRoot;

    /**
     * feature 输出根目录。
     */
    private String featureRoot;

    /**
     * 扫描样本数量。
     */
    private Integer scannedCount;

    /**
     * 成功转换数量。
     */
    private Integer convertedCount;

    /**
     * 跳过数量。
     */
    private Integer skippedCount;

    /**
     * 失败数量。
     */
    private Integer failedCount;

    /**
     * 失败项。
     */
    private List<Map<String, Object>> failedItems;

    /**
     * 结果说明。
     */
    private String message;
}
