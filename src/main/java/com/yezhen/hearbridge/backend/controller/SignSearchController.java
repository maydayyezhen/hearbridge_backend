package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.SignSearchResult;
import com.yezhen.hearbridge.backend.service.SignSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手语训练统一搜索 Controller。
 */
@RestController
@RequestMapping("/sign/search")
public class SignSearchController {

    /**
     * 手语训练统一搜索服务。
     */
    private final SignSearchService signSearchService;

    /**
     * 构造注入搜索服务。
     *
     * @param signSearchService 手语训练统一搜索服务
     */
    public SignSearchController(SignSearchService signSearchService) {
        this.signSearchService = signSearchService;
    }

    /**
     * 搜索分类和手势资源。
     *
     * @param keyword  搜索关键词
     * @param pageNo   当前页码，从 1 开始
     * @param pageSize 每页数量
     * @return 统一搜索结果
     */
    @GetMapping
    public SignSearchResult search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return signSearchService.search(keyword, pageNo, pageSize);
    }
}
