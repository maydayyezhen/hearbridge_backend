package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 手势样本质量状态更新请求。
 */
@Getter
@Setter
public class SignSampleQualityUpdateRequest {

    /**
     * 质量状态。
     *
     * 可选值：
     * UNKNOWN、GOOD、WARNING、BAD
     */
    private String qualityStatus;

    /**
     * 质量说明。
     */
    private String qualityMessage;
}
