package com.yezhen.hearbridge.backend.dto;

import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 手语训练统一搜索结果。
 *
 * 当前 1.0 版本说明：
 * 1. categories 返回匹配的分类。
 * 2. resources 返回匹配的具体手势资源。
 * 3. 后续分页阶段可进一步拆成 PageResult 或追加 pageNo / pageSize / total。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignSearchResult {

    /**
     * 搜索关键词。
     */
    private String keyword;

    /**
     * 匹配分类列表。
     */
    private List<SignCategory> categories;

    /**
     * 匹配手势资源列表。
     */
    private List<SignResource> resources;
}
