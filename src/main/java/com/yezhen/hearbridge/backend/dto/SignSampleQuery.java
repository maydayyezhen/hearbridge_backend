package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 手势样本查询参数。
 */
@Getter
@Setter
public class SignSampleQuery {

    /**
     * 资源编码。
     */
    private String resourceCode;

    /**
     * 质量状态。
     *
     * 可选值：
     * UNKNOWN、GOOD、WARNING、BAD
     */
    private String qualityStatus;

    /**
     * 是否删除。
     *
     * null 时，Service 层默认查询未删除数据。
     */
    private Boolean deleted;

    /**
     * 当前页码，从 1 开始。
     */
    private Integer page;

    /**
     * 每页数量。
     */
    private Integer pageSize;
}
