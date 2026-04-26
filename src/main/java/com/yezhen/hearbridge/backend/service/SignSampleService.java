package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleItem;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;

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
     * 构造注入手势样本 Mapper。
     *
     * @param signSampleMapper 手势样本 Mapper
     */
    public SignSampleService(
            SignSampleMapper signSampleMapper,
            PythonGestureServiceClient pythonGestureServiceClient) {
        this.signSampleMapper = signSampleMapper;
        this.pythonGestureServiceClient = pythonGestureServiceClient;
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

}
