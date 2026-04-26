package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
     * 构造注入手势样本 Mapper。
     *
     * @param signSampleMapper 手势样本 Mapper
     */
    public SignSampleService(SignSampleMapper signSampleMapper) {
        this.signSampleMapper = signSampleMapper;
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
}
