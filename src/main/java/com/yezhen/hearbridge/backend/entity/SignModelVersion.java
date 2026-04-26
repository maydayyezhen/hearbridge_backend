package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手势识别模型版本实体。
 *
 * 用于登记每次训练生成的模型文件、标签映射、评估结果和发布状态。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignModelVersion {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 模型版本名称。
     */
    private String versionName;

    /**
     * Python 训练运行名称。
     */
    private String runName;

    /**
     * 模型文件路径。
     */
    private String modelPath;

    /**
     * 标签映射文件路径。
     */
    private String labelMapPath;

    /**
     * 训练曲线图路径。
     */
    private String trainingCurvePath;

    /**
     * 混淆矩阵图路径。
     */
    private String confusionMatrixPath;

    /**
     * 评估结果文本路径。
     */
    private String evalResultPath;

    /**
     * 样本总数。
     */
    private Integer sampleCount;

    /**
     * 训练集样本数。
     */
    private Integer trainSampleCount;

    /**
     * 验证集样本数。
     */
    private Integer valSampleCount;

    /**
     * 类别数量。
     */
    private Integer classCount;

    /**
     * 输入形状。
     *
     * 例如：30x166
     */
    private String inputShape;

    /**
     * 最终训练准确率。
     */
    private BigDecimal finalTrainAccuracy;

    /**
     * 最终验证准确率。
     */
    private BigDecimal finalValAccuracy;

    /**
     * 最终训练损失。
     */
    private BigDecimal finalTrainLoss;

    /**
     * 最终验证损失。
     */
    private BigDecimal finalValLoss;

    /**
     * 训练耗时，单位秒。
     */
    private BigDecimal durationSec;

    /**
     * 标签映射 JSON。
     */
    private String labelMapJson;

    /**
     * 模型状态。
     *
     * 可选值：
     * TRAINED、PUBLISHED、DISABLED
     */
    private String status;

    /**
     * 是否为当前发布版本。
     */
    private Boolean published;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
