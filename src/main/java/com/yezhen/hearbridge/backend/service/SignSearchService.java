package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.PageResult;
import com.yezhen.hearbridge.backend.dto.SignSearchResult;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.util.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 手语训练统一搜索 Service。
 *
 * 当前分页版说明：
 * 1. 先复用现有分类 / 资源列表能力，并在内存中做关键词过滤和分页。
 * 2. 后续数据量变大后，再下沉到 Mapper / SQL 层。
 * 3. 搜索范围包括分类 code / nameZh、资源 code / nameZh / categoryCode。
 */
@Service
public class SignSearchService {

    /**
     * 手语分类服务。
     */
    private final SignCategoryService signCategoryService;

    /**
     * 手语资源服务。
     */
    private final SignResourceService signResourceService;

    /**
     * 构造注入依赖。
     *
     * @param signCategoryService 手语分类服务
     * @param signResourceService 手语资源服务
     */
    public SignSearchService(
            SignCategoryService signCategoryService,
            SignResourceService signResourceService) {
        this.signCategoryService = signCategoryService;
        this.signResourceService = signResourceService;
    }

    /**
     * 搜索分类和手势资源。
     *
     * @param keyword 搜索关键词
     * @param pageNo 当前页码
     * @param pageSize 每页数量
     * @return 统一搜索结果
     */
    public SignSearchResult search(String keyword, Integer pageNo, Integer pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePageNo = PageUtils.normalizePageNo(pageNo);
        int safePageSize = PageUtils.normalizePageSize(pageSize);
        int offset = (safePageNo - 1) * safePageSize;

        if (!StringUtils.hasText(normalizedKeyword)) {
            return new SignSearchResult(
                    "",
                    PageResult.of(new ArrayList<>(), 0, safePageNo, safePageSize),
                    PageResult.of(new ArrayList<>(), 0, safePageNo, safePageSize)
            );
        }

        long categoryTotal = signCategoryService.countByKeyword(normalizedKeyword);
        List<SignCategory> categories = signCategoryService.searchPage(
                normalizedKeyword,
                offset,
                safePageSize
        );

        long resourceTotal = signResourceService.countByKeyword(normalizedKeyword);
        List<SignResource> resources = signResourceService.searchPage(
                normalizedKeyword,
                offset,
                safePageSize
        );

        return new SignSearchResult(
                normalizedKeyword,
                PageResult.of(categories, categoryTotal, safePageNo, safePageSize),
                PageResult.of(resources, resourceTotal, safePageNo, safePageSize)
        );
    }

    /**
     * 兼容旧调用：搜索分类和手势资源。
     *
     * @param keyword 搜索关键词
     * @return 统一搜索结果
     */
    public SignSearchResult search(String keyword) {
        return search(keyword, null, null);
    }


    /**
     * 规范化关键词。
     *
     * @param keyword 原始关键词
     * @return 小写且去除首尾空白后的关键词
     */
    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
