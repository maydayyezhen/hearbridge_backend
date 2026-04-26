package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 样本同步结果。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignSampleSyncResult {

    /**
     * Python 扫描到的样本数量。
     */
    private Integer scannedCount;

    /**
     * 新增数量。
     */
    private Integer insertedCount;

    /**
     * 更新数量。
     */
    private Integer updatedCount;

    /**
     * 跳过数量。
     */
    private Integer skippedCount;

    /**
     * 异常样本数量。
     */
    private Integer badCount;
}
