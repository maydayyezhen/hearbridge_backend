package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.service.SignCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手语资源分类 Controller。
 *
 * 当前阶段说明：
 * 1. 管理端第二阶段先补齐分类 CRUD。
 * 2. 暂时继续直接使用 SignCategory 作为请求体和响应体。
 * 3. 后续如果管理端接口变复杂，再单独抽 CreateRequest / UpdateRequest / Response DTO。
 */
@RestController
@RequestMapping("/sign/categories")
public class SignCategoryController {

    /**
     * 手语资源分类业务服务。
     */
    private final SignCategoryService signCategoryService;

    /**
     * 构造注入手语资源分类业务服务。
     *
     * @param signCategoryService 手语资源分类业务服务
     */
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

    /**
     * 新增手语资源分类。
     *
     * @param category 分类信息
     * @return 新增后的分类信息
     */
    @PostMapping
    public SignCategory createCategory(@RequestBody SignCategory category) {
        return signCategoryService.create(category);
    }

    /**
     * 更新手语资源分类。
     *
     * 注意：
     * 这里使用 id 作为更新目标，code 只是业务编码字段。
     *
     * @param id       分类主键 ID
     * @param category 分类更新信息
     * @return 更新后的分类信息
     */
    @PutMapping("/{id}")
    public SignCategory updateCategory(
            @PathVariable("id") Long id,
            @RequestBody SignCategory category) {
        return signCategoryService.update(id, category);
    }

    /**
     * 删除手语资源分类。
     *
     * @param id 分类主键 ID
     */
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable("id") Long id) {
        signCategoryService.deleteById(id);
    }
}
