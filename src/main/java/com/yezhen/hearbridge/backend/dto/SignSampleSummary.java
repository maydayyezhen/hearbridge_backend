package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手势样本统计结果。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignSampleSummary {

    /**
     * 样本总数。
     */
    private Long totalCount;

    /**
     * 覆盖的资源数量。
     */
    private Long resourceCount;

    /**
     * 高质量样本数量。
     */
    private Long goodCount;

    /**
     * 警告样本数量。
     */
    private Long warningCount;

    /**
     * 异常样本数量。
     */
    private Long badCount;

    /**
     * 未知质量样本数量。
     */
    private Long unknownCount;
}
