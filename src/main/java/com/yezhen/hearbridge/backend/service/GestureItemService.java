package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.entity.GestureItem;
import com.yezhen.hearbridge.backend.mapper.GestureItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 手势 Service
 */
@Service
public class GestureItemService {

    private final GestureItemMapper gestureItemMapper;

    public GestureItemService(GestureItemMapper gestureItemMapper) {
        this.gestureItemMapper = gestureItemMapper;
    }

    /**
     * 根据分类ID查询手势列表
     *
     * @param categoryId 分类ID
     * @return 手势列表
     */
    public List<GestureItem> listByCategoryId(Long categoryId) {
        return gestureItemMapper.selectByCategoryId(categoryId);
    }
}
