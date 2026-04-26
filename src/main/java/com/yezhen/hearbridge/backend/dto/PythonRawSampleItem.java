package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Python 服务返回的 raw 样本摘要项。
 */
@Getter
@Setter
public class PythonRawSampleItem {

    /**
     * 样本唯一编码。
     */
    private String sampleCode;

    /**
     * 资源编码。
     */
    private String resourceCode;

    /**
     * 训练标签。
     */
    private String label;

    /**
     * raw 文件路径。
     */
    private String rawFilePath;

    /**
     * 样本帧数。
     */
    private Integer frameCount;

    /**
     * 样本时长，单位毫秒。
     */
    private Integer durationMs;

    /**
     * 估算 FPS。
     */
    private BigDecimal fps;

    /**
     * 手部检测比例。
     */
    private BigDecimal handPresentRatio;

    /**
     * 姿态检测比例。
     */
    private BigDecimal posePresentRatio;

    /**
     * Pose 是否归一化。
     */
    private Boolean poseNormalized;

    /**
     * 质量状态。
     */
    private String qualityStatus;

    /**
     * 质量说明。
     */
    private String qualityMessage;
}
