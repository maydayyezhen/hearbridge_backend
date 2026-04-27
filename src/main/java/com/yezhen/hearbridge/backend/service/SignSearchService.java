package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SignSearchResult;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 手语训练统一搜索 Service。
 *
 * 当前 1.0 版本说明：
 * 1. 先复用现有分类 / 资源列表能力，并在内存中做关键词过滤。
 * 2. 后续数据量变大和接入分页后，再下沉到 Mapper / SQL 层。
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
     * @return 统一搜索结果
     */
    public SignSearchResult search(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return new SignSearchResult("", new ArrayList<>(), new ArrayList<>());
        }

        List<SignCategory> matchedCategories = searchCategories(normalizedKeyword);
        List<SignResource> matchedResources = searchResources(normalizedKeyword);

        return new SignSearchResult(normalizedKeyword, matchedCategories, matchedResources);
    }

    /**
     * 搜索分类。
     *
     * @param keyword 已规范化关键词
     * @return 匹配分类列表
     */
    private List<SignCategory> searchCategories(String keyword) {
        List<SignCategory> result = new ArrayList<>();
        List<SignCategory> categories = signCategoryService.listAll();

        for (SignCategory category : categories) {
            if (category == null) {
                continue;
            }

            if (matches(keyword, category.getCode()) || matches(keyword, category.getNameZh())) {
                result.add(category);
            }
        }

        return result;
    }

    /**
     * 搜索手势资源。
     *
     * @param keyword 已规范化关键词
     * @return 匹配手势资源列表
     */
    private List<SignResource> searchResources(String keyword) {
        List<SignResource> result = new ArrayList<>();
        List<SignResource> resources = signResourceService.list(null);

        for (SignResource resource : resources) {
            if (resource == null) {
                continue;
            }

            if (matches(keyword, resource.getCode()) ||
                    matches(keyword, resource.getNameZh()) ||
                    matches(keyword, resource.getCategoryCode())) {
                result.add(resource);
            }
        }

        return result;
    }

    /**
     * 判断字段是否匹配关键词。
     *
     * @param keyword 已规范化关键词
     * @param value   字段值
     * @return 是否匹配
     */
    private boolean matches(String keyword, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(keyword);
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
