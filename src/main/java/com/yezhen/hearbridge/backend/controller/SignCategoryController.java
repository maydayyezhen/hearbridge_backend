package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.service.SignCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手语资源分类 Controller。
 */
@RestController
@RequestMapping("/sign/categories")
public class SignCategoryController {

    private final SignCategoryService signCategoryService;

    public SignCategoryController(SignCategoryService signCategoryService) {
        this.signCategoryService = signCategoryService;
    }

    /**
     * 查询全部分类。
     *
     * @return 分类列表
     */
    @GetMapping
    public List<SignCategory> listCategories() {
        return signCategoryService.listAll();
    }

    /**
     * 根据编码查询分类详情。
     *
     * @param code 分类编码
     * @return 分类详情
     */
    @GetMapping("/{code}")
    public SignCategory getCategory(@PathVariable("code") String code) {
        return signCategoryService.getByCode(code);
    }
}
