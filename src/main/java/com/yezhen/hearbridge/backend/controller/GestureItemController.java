package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.GestureItem;
import com.yezhen.hearbridge.backend.service.GestureItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手势 Controller
 */
@RestController
public class GestureItemController {

    private final GestureItemService gestureItemService;

    public GestureItemController(GestureItemService gestureItemService) {
        this.gestureItemService = gestureItemService;
    }

    /**
     * 根据分类ID查询手势列表
     *
     * @param categoryId 分类ID
     * @return 手势列表
     */
    @GetMapping("/gesture/items")
    public List<GestureItem> listItemsByCategoryId(@RequestParam("categoryId") Long categoryId) {
        return gestureItemService.listByCategoryId(categoryId);
    }
}
