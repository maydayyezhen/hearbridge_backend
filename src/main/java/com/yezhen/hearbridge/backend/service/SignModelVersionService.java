package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.mapper.SignModelVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.yezhen.hearbridge.backend.dto.ModelReloadResult;
import com.yezhen.hearbridge.backend.config.MinioProperties;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型版本 Service。
 *
 * 职责：
 * 1. 保存模型训练结果；
 * 2. 查询模型版本列表；
 * 3. 查询当前发布版本；
 * 4. 发布指定模型版本。
 */
@Service
public class SignModelVersionService {

    /**
     * 已训练状态。
     */
    private static final String STATUS_TRAINED = "TRAINED";

    /**
     * 已发布状态。
     */
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    /**
     * 模型版本 Mapper。
     */
    private final SignModelVersionMapper signModelVersionMapper;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Python 手势识别服务客户端。
     */
    private final PythonGestureServiceClient pythonGestureServiceClient;

    /**
     * MinIO 配置。
     */
    private final MinioProperties minioProperties;

    /**
     * 构造注入依赖。
     *
     * @param signModelVersionMapper 模型版本 Mapper
     * @param objectMapper           JSON 序列化工具
     */
    public SignModelVersionService(
            SignModelVersionMapper signModelVersionMapper,
            ObjectMapper objectMapper,
            PythonGestureServiceClient pythonGestureServiceClient,
            MinioProperties minioProperties) {
        this.signModelVersionMapper = signModelVersionMapper;
        this.objectMapper = objectMapper;
        this.pythonGestureServiceClient = pythonGestureServiceClient;
        this.minioProperties = minioProperties;
    }

    /**
     * 查询模型版本列表。
     *
     * @return 模型版本列表
     */
    public List<SignModelVersion> listAll() {
        return signModelVersionMapper.selectAll()
                .stream()
                .map(this::attachArtifactUrls)
                .collect(Collectors.toList());
    }

    /**
     * 查询当前发布模型版本。
     *
     * @return 当前发布版本；没有发布版本时返回 null
     */
    public SignModelVersion getPublished() {
        return attachArtifactUrls(signModelVersionMapper.selectPublished());
    }

    /**
     * 根据训练结果创建模型版本。
     *
     * @param trainResult Python 训练结果
     * @return 已创建的模型版本
     */
    public SignModelVersion createFromTrainResult(ModelTrainResult trainResult) {
        validateTrainResult(trainResult);

        SignModelVersion existed = signModelVersionMapper.selectByRunName(trainResult.getRunName());
        if (existed != null) {
            return existed;
        }

        SignModelVersion version = new SignModelVersion();

        version.setVersionName(trainResult.getRunName());
        version.setRunName(trainResult.getRunName());

        version.setModelPath(trainResult.getModelPath());
        version.setLabelMapPath(trainResult.getLabelMapPath());
        version.setTrainingCurvePath(trainResult.getTrainingCurvePath());
        version.setConfusionMatrixPath(trainResult.getConfusionMatrixPath());
        version.setEvalResultPath(trainResult.getEvalResultPath());

        version.setSampleCount(trainResult.getSampleCount());
        version.setTrainSampleCount(trainResult.getTrainSampleCount());
        version.setValSampleCount(trainResult.getValSampleCount());
        version.setClassCount(trainResult.getClassCount());
        version.setInputShape(formatInputShape(trainResult));

        version.setFinalTrainAccuracy(trainResult.getFinalTrainAccuracy());
        version.setFinalValAccuracy(trainResult.getFinalValAccuracy());
        version.setFinalTrainLoss(trainResult.getFinalTrainLoss());
        version.setFinalValLoss(trainResult.getFinalValLoss());
        version.setDurationSec(trainResult.getDurationSec());

        version.setLabelMapJson(serializeLabelMap(trainResult));
        version.setStatus(STATUS_TRAINED);
        version.setPublished(false);

        signModelVersionMapper.insert(version);

        return signModelVersionMapper.selectById(version.getId());
    }

    /**
     * 发布指定模型版本。
     *
     * 第一版发布策略：
     * 1. 先调用 Python 服务重载模型；
     * 2. Python 重载成功后，再更新数据库发布状态；
     * 3. 避免出现数据库显示已发布、但 Python 实际未加载的状态。
     *
     * @param id 模型版本 ID
     * @return 发布后的模型版本
     */
    public SignModelVersion publish(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("模型版本 ID 不能为空");
        }

        SignModelVersion version = signModelVersionMapper.selectById(id);
        if (version == null) {
            throw new IllegalArgumentException("模型版本不存在，ID：" + id);
        }

        ModelReloadResult reloadResult;

        if (isModelArtifactObjectKey(version.getModelPath())
                && isModelArtifactObjectKey(version.getLabelMapPath())) {
            reloadResult = pythonGestureServiceClient.reloadModelFromUrl(
                    minioProperties.buildObjectUrl(version.getModelPath()),
                    minioProperties.buildObjectUrl(version.getLabelMapPath()),
                    version.getVersionName()
            );
        } else {
            // 兼容历史版本：旧数据里可能仍然是 Python 本地路径。
            reloadResult = pythonGestureServiceClient.reloadModel(
                    version.getModelPath(),
                    version.getLabelMapPath(),
                    version.getVersionName()
            );
        }

        if (reloadResult == null || !Boolean.TRUE.equals(reloadResult.getOk())) {
            throw new IllegalArgumentException("Python 服务重载模型失败");
        }

        signModelVersionMapper.clearPublished();
        signModelVersionMapper.publishById(id);

        SignModelVersion published = signModelVersionMapper.selectById(id);
        if (published == null) {
            throw new IllegalArgumentException("模型版本发布失败，ID：" + id);
        }

        published.setStatus(STATUS_PUBLISHED);
        published.setPublished(true);

        return attachArtifactUrls(published);
    }

    /**
     * 校验训练结果。
     *
     * @param trainResult 训练结果
     */
    private void validateTrainResult(ModelTrainResult trainResult) {
        if (trainResult == null) {
            throw new IllegalArgumentException("模型训练结果不能为空");
        }

        if (!StringUtils.hasText(trainResult.getRunName())) {
            throw new IllegalArgumentException("训练运行名称不能为空");
        }

        if (!StringUtils.hasText(trainResult.getModelPath())) {
            throw new IllegalArgumentException("模型文件路径不能为空");
        }

        if (!StringUtils.hasText(trainResult.getLabelMapPath())) {
            throw new IllegalArgumentException("标签映射文件路径不能为空");
        }
    }

    /**
     * 格式化输入形状。
     *
     * @param trainResult 训练结果
     * @return 输入形状字符串
     */
    private String formatInputShape(ModelTrainResult trainResult) {
        if (trainResult.getInputShape() == null || trainResult.getInputShape().isEmpty()) {
            return "";
        }

        return trainResult.getInputShape()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining("x"));
    }

    /**
     * 序列化标签映射。
     *
     * @param trainResult 训练结果
     * @return 标签映射 JSON
     */
    private String serializeLabelMap(ModelTrainResult trainResult) {
        if (trainResult.getLabelMap() == null) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(trainResult.getLabelMap());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("标签映射序列化失败：" + exception.getMessage());
        }
    }

    /**
     * 判断是否为模型训练产物 objectKey。
     *
     * @param value 路径或对象 Key
     * @return 是否为 MinIO 模型训练产物 objectKey
     */
    private boolean isModelArtifactObjectKey(String value) {
        return StringUtils.hasText(value)
                && value.startsWith("sign/model/artifacts/");
    }

    /**
     * 为模型版本附加 MinIO 访问 URL。
     *
     * @param version 模型版本
     * @return 附加 URL 后的模型版本
     */
    private SignModelVersion attachArtifactUrls(SignModelVersion version) {
        if (version == null) {
            return null;
        }

        version.setModelUrl(buildUrlIfObjectKey(version.getModelPath()));
        version.setLabelMapUrl(buildUrlIfObjectKey(version.getLabelMapPath()));
        version.setTrainingCurveUrl(buildUrlIfObjectKey(version.getTrainingCurvePath()));
        version.setConfusionMatrixUrl(buildUrlIfObjectKey(version.getConfusionMatrixPath()));
        version.setEvalResultUrl(buildUrlIfObjectKey(version.getEvalResultPath()));

        return version;
    }

    /**
     * 如果字段值是 MinIO objectKey，则生成访问 URL。
     *
     * @param value 路径或对象 Key
     * @return 访问 URL；不是 objectKey 时返回 null
     */
    private String buildUrlIfObjectKey(String value) {
        if (!isModelArtifactObjectKey(value)) {
            return null;

        }

        return minioProperties.buildObjectUrl(value);
    }

}
