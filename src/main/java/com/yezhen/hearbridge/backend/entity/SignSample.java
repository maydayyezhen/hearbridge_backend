package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手势训练样本元数据实体。
 *
 * 说明：
 * 1. raw 样本文件当前仍由 Python 服务保存；
 * 2. Spring Boot 当前只维护样本元数据；
 * 3. 后续可以通过 rawObjectKey 扩展为 MinIO 存储。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignSample {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 样本唯一编码。
     */
    private String sampleCode;

    /**
     * 关联的手势资源编码。
     */
    private String resourceCode;

    /**
     * 训练标签。
     *
     * 第一版通常与 resourceCode 保持一致。
     */
    private String label;

    /**
     * Python 服务中的 raw 样本文件路径。
     */
    private String rawFilePath;

    /**
     * raw 样本文件在 MinIO 中的对象 Key。
     *
     * 当前第一版可以为空，后续迁移到 MinIO 时再使用。
     */
    private String rawObjectKey;

    /**
     * 样本帧数。
     */
    private Integer frameCount;

    /**
     * 样本时长，单位毫秒。
     */
    private Integer durationMs;

    /**
     * 估算帧率。
     */
    private BigDecimal fps;

    /**
     * 检测到手部的帧比例。
     */
    private BigDecimal handPresentRatio;

    /**
     * 检测到人体姿态的帧比例。
     */
    private BigDecimal posePresentRatio;

    /**
     * Pose 是否已归一化。
     */
    private Boolean poseNormalized;

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

    /**
     * 是否删除。
     */
    private Boolean deleted;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
