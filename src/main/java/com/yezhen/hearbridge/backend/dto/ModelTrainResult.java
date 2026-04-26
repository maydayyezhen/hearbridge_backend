package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Python 模型训练结果。
 */
@Getter
@Setter
public class ModelTrainResult {

    /**
     * 训练运行名称。
     */
    private String runName;

    /**
     * 训练数据目录。
     */
    private String dataRoot;

    /**
     * 训练产物目录。
     */
    private String artifactDir;

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
     */
    private List<Integer> inputShape;

    /**
     * 实际训练轮数。
     */
    private Integer epochsRan;

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
     * 标签映射。
     */
    private Map<String, Integer> labelMap;

    /**
     * 结果说明。
     */
    private String message;

    /**
     * 后端登记后的模型版本 ID。
     */
    private Long versionId;

    /**
     * 后端登记后的模型版本名称。
     */
    private String versionName;

    /**
     * 后端登记后的模型版本状态。
     */
    private String versionStatus;

    /**
     * 是否为当前发布版本。
     */
    private Boolean published;
}
