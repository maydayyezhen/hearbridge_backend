package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleItem;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.dto.FileUploadResult;

import java.io.ByteArrayInputStream;

import java.util.Set;

/**
 * 手势样本 Service。
 *
 * 当前第一版职责：
 * 1. 查询样本列表；
 * 2. 查询样本统计；
 * 3. 软删除样本；
 * 4. 更新样本质量状态。
 */
@Service
public class SignSampleService {

    /**
     * 允许的质量状态集合。
     */
    private static final Set<String> ALLOWED_QUALITY_STATUS = Set.of(
            "UNKNOWN",
            "GOOD",
            "WARNING",
            "BAD"
    );

    /**
     * 默认页码。
     */
    private static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页数量。
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页数量。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 手势样本 Mapper。
     */
    private final SignSampleMapper signSampleMapper;

    /**
     * Python 手势识别服务客户端。
     */
    private final PythonGestureServiceClient pythonGestureServiceClient;

    /**
     * 模型版本 Service。
     */
    private final SignModelVersionService signModelVersionService;

    /**
     * MinIO 文件存储服务。
     */
    private final MinioStorageService minioStorageService;

    /**
     * 构造注入手势样本 Mapper。
     *
     * @param signSampleMapper 手势样本 Mapper
     */
    public SignSampleService(
            SignSampleMapper signSampleMapper,
            PythonGestureServiceClient pythonGestureServiceClient,
            SignModelVersionService signModelVersionService,
            MinioStorageService minioStorageService) {
        this.signSampleMapper = signSampleMapper;
        this.pythonGestureServiceClient = pythonGestureServiceClient;
        this.signModelVersionService = signModelVersionService;
        this.minioStorageService = minioStorageService;
    }

    /**
     * 分页查询样本列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public SignSamplePageResult list(SignSampleQuery query) {
        SignSampleQuery normalizedQuery = normalizeQuery(query);

        int page = normalizedQuery.getPage();
        int pageSize = normalizedQuery.getPageSize();
        int offset = (page - 1) * pageSize;

        Long total = signSampleMapper.countPage(normalizedQuery);

        return new SignSamplePageResult(
                signSampleMapper.selectPage(normalizedQuery, offset, pageSize),
                total,
                page,
                pageSize
        );
    }

    /**
     * 查询样本统计信息。
     *
     * @return 样本统计信息
     */
    public SignSampleSummary summary() {
        SignSampleSummary summary = signSampleMapper.selectSummary();

        if (summary == null) {
            return new SignSampleSummary(0L, 0L, 0L, 0L, 0L, 0L);
        }

        return summary;
    }

    /**
     * 软删除样本。
     *
     * @param id 样本 ID
     */
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("样本 ID 不能为空");
        }

        SignSample sample = signSampleMapper.selectById(id);
        if (sample == null) {
            throw new IllegalArgumentException("样本不存在，ID：" + id);
        }

        if (Boolean.TRUE.equals(sample.getDeleted())) {
            throw new IllegalArgumentException("样本已删除，ID：" + id);
        }

        signSampleMapper.softDeleteById(id);
    }

    /**
     * 更新样本质量状态。
     *
     * @param id      样本 ID
     * @param request 质量状态更新请求
     * @return 更新后的样本信息
     */
    public SignSample updateQuality(Long id, SignSampleQualityUpdateRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("样本 ID 不能为空");
        }

        if (request == null) {
            throw new IllegalArgumentException("质量更新请求不能为空");
        }

        validateQualityStatus(request.getQualityStatus());

        SignSample sample = signSampleMapper.selectById(id);
        if (sample == null) {
            throw new IllegalArgumentException("样本不存在，ID：" + id);
        }

        if (Boolean.TRUE.equals(sample.getDeleted())) {
            throw new IllegalArgumentException("样本已删除，不能修改质量状态，ID：" + id);
        }

        signSampleMapper.updateQuality(
                id,
                request.getQualityStatus(),
                request.getQualityMessage()
        );

        return signSampleMapper.selectById(id);
    }

    /**
     * 归一化查询参数。
     *
     * @param query 原始查询参数
     * @return 归一化后的查询参数
     */
    private SignSampleQuery normalizeQuery(SignSampleQuery query) {
        SignSampleQuery normalizedQuery = query == null ? new SignSampleQuery() : query;

        if (normalizedQuery.getDeleted() == null) {
            normalizedQuery.setDeleted(false);
        }

        Integer page = normalizedQuery.getPage();
        if (page == null || page < 1) {
            normalizedQuery.setPage(DEFAULT_PAGE);
        }

        Integer pageSize = normalizedQuery.getPageSize();
        if (pageSize == null || pageSize < 1) {
            normalizedQuery.setPageSize(DEFAULT_PAGE_SIZE);
        } else if (pageSize > MAX_PAGE_SIZE) {
            normalizedQuery.setPageSize(MAX_PAGE_SIZE);
        }

        if (StringUtils.hasText(normalizedQuery.getQualityStatus())) {
            validateQualityStatus(normalizedQuery.getQualityStatus());
        }

        return normalizedQuery;
    }

    /**
     * 校验质量状态是否合法。
     *
     * @param qualityStatus 质量状态
     */
    private void validateQualityStatus(String qualityStatus) {
        if (!StringUtils.hasText(qualityStatus)) {
            throw new IllegalArgumentException("质量状态不能为空");
        }

        if (!ALLOWED_QUALITY_STATUS.contains(qualityStatus)) {
            throw new IllegalArgumentException("不支持的质量状态：" + qualityStatus);
        }
    }

    /**
     * 从 Python 服务同步 raw 样本摘要到 MySQL。
     *
     * @return 同步结果
     */
    public SignSampleSyncResult syncFromPythonRawDataset() {
        PythonRawSampleListResponse response = pythonGestureServiceClient.listRawSamples();

        if (response == null || response.getItems() == null) {
            return new SignSampleSyncResult(0, 0, 0, 0, 0);
        }

        int insertedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int badCount = 0;

        for (PythonRawSampleItem item : response.getItems()) {
            if (item == null || !StringUtils.hasText(item.getSampleCode())) {
                skippedCount++;
                continue;
            }

            SignSample sample = convertPythonItemToSample(item);

            if ("BAD".equals(sample.getQualityStatus())) {
                badCount++;
            }

            SignSample existed = signSampleMapper.selectBySampleCode(sample.getSampleCode());
            if (existed == null) {
                signSampleMapper.insert(sample);
                insertedCount++;
            } else {
                signSampleMapper.updateBySampleCode(sample);
                updatedCount++;
            }
        }

        return new SignSampleSyncResult(
                response.getItems().size(),
                insertedCount,
                updatedCount,
                skippedCount,
                badCount
        );
    }

    /**
     * 将 Python 样本摘要转换为数据库实体。
     *
     * @param item Python 样本摘要
     * @return SignSample 实体
     */
    private SignSample convertPythonItemToSample(PythonRawSampleItem item) {
        SignSample sample = new SignSample();

        sample.setSampleCode(item.getSampleCode());
        sample.setResourceCode(item.getResourceCode());
        sample.setLabel(item.getLabel());
        sample.setRawFilePath(item.getRawFilePath());
        sample.setRawObjectKey(null);
        sample.setFrameCount(item.getFrameCount());
        sample.setDurationMs(item.getDurationMs());
        sample.setFps(item.getFps());
        sample.setHandPresentRatio(item.getHandPresentRatio());
        sample.setPosePresentRatio(item.getPosePresentRatio());
        sample.setPoseNormalized(item.getPoseNormalized());
        sample.setQualityStatus(
                StringUtils.hasText(item.getQualityStatus())
                        ? item.getQualityStatus()
                        : "UNKNOWN"
        );
        sample.setQualityMessage(item.getQualityMessage());
        sample.setDeleted(false);

        return sample;
    }
    /**
     * 调用 Python 服务执行 raw → feature 转换。
     *
     * @return 转换结果
     */
    public FeatureConvertResult convertRawToFeatures() {
        FeatureConvertResult result = pythonGestureServiceClient.convertRawToFeatures();

        if (result == null) {
            throw new IllegalArgumentException("raw → feature 转换失败：Python 服务未返回结果");
        }

        return result;
    }

    /**
     * 调用 Python 服务执行模型训练，并自动登记模型版本。
     *
     * @return 模型训练结果
     */
    /**
     * 调用 Python 服务执行模型训练，并自动登记模型版本。
     *
     * @return 模型训练结果
     */
    public ModelTrainResult trainModel() {
        ModelTrainResult result = pythonGestureServiceClient.trainModel();

        if (result == null) {
            throw new IllegalArgumentException("模型训练失败：Python 服务未返回结果");
        }

        uploadTrainArtifactsToMinio(result);

        SignModelVersion version = signModelVersionService.createFromTrainResult(result);

        if (version != null) {
            result.setVersionId(version.getId());
            result.setVersionName(version.getVersionName());
            result.setVersionStatus(version.getStatus());
            result.setPublished(version.getPublished());
        }

        return result;
    }

    /**
     * 将 Python 训练产物上传到 MinIO，并将训练结果中的路径字段替换为 MinIO objectKey。
     *
     * @param result Python 训练结果
     */
    private void uploadTrainArtifactsToMinio(ModelTrainResult result) {
        if (result == null || !StringUtils.hasText(result.getRunName())) {
            throw new IllegalArgumentException("训练结果或 runName 不能为空");
        }

        String runName = result.getRunName();

        result.setModelPath(uploadTrainArtifact(
                runName,
                extractFileName(result.getModelPath(), "gesture_cnn_arm_pose_10fps.keras"),
                "application/octet-stream"
        ));

        result.setLabelMapPath(uploadTrainArtifact(
                runName,
                extractFileName(result.getLabelMapPath(), "label_map_arm_pose_10fps.json"),
                "application/json"
        ));

        result.setTrainingCurvePath(uploadTrainArtifact(
                runName,
                extractFileName(result.getTrainingCurvePath(), "training_curve.png"),
                "image/png"
        ));

        result.setConfusionMatrixPath(uploadTrainArtifact(
                runName,
                extractFileName(result.getConfusionMatrixPath(), "confusion_matrix.png"),
                "image/png"
        ));

        result.setEvalResultPath(uploadTrainArtifact(
                runName,
                extractFileName(result.getEvalResultPath(), "eval_result.txt"),
                "text/plain"
        ));
    }

    /**
     * 下载单个 Python 训练产物并上传到 MinIO。
     *
     * @param runName     训练运行名称
     * @param fileName    文件名
     * @param contentType 文件类型
     * @return MinIO objectKey
     */
    private String uploadTrainArtifact(String runName, String fileName, String contentType) {
        byte[] bytes = pythonGestureServiceClient.downloadArtifact(runName, fileName);

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("训练产物为空：" + runName + "/" + fileName);
        }

        String objectKey = "sign/model/artifacts/" + runName + "/" + fileName;

        FileUploadResult uploadResult = minioStorageService.uploadObject(
                new ByteArrayInputStream(bytes),
                bytes.length,
                objectKey,
                contentType,
                fileName
        );

        return uploadResult.getObjectKey();
    }

    /**
     * 从路径中提取文件名。
     *
     * @param path         原始路径
     * @param fallbackName 兜底文件名
     * @return 文件名
     */
    private String extractFileName(String path, String fallbackName) {
        if (!StringUtils.hasText(path)) {
            return fallbackName;
        }

        String normalizedPath = path.replace("\\", "/");
        int index = normalizedPath.lastIndexOf("/");

        if (index < 0 || index >= normalizedPath.length() - 1) {
            return normalizedPath;
        }

        return normalizedPath.substring(index + 1);
    }

}
