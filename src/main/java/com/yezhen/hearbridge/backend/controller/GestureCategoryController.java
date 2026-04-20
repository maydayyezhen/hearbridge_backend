package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.GestureCategory;
import com.yezhen.hearbridge.backend.service.GestureCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手势分类 Controller
 */
@RestController
public class GestureCategoryController {

    private final GestureCategoryService gestureCategoryService;

    public GestureCategoryController(GestureCategoryService gestureCategoryService) {
        this.gestureCategoryService = gestureCategoryService;
    }

    /**
     * 查询全部手势分类
     *
     * @return 手势分类列表
     */
    @GetMapping("/gesture/categories")
    public List<GestureCategory> listCategories() {
        return gestureCategoryService.listAll();
    }
}
