package com.yezhen.hearbridge.backend.dto;

import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手语训练统一搜索结果。
 *
 * 当前分页版说明：
 * 1. categories 返回匹配分类的分页结果。
 * 2. resources 返回匹配手势资源的分页结果。
 * 3. 当前分类和资源共用同一组 pageNo / pageSize 参数，后续如需更细可拆分 categoryPageNo / resourcePageNo。
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
     * 匹配分类分页结果。
     */
    private PageResult<SignCategory> categories;

    /**
     * 匹配手势资源分页结果。
     */
    private PageResult<SignResource> resources;
}
