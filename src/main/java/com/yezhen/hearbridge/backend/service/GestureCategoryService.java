package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.entity.GestureCategory;
import com.yezhen.hearbridge.backend.mapper.GestureCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 手势分类 Service
 */
@Service
public class GestureCategoryService {

    private final GestureCategoryMapper gestureCategoryMapper;

    public GestureCategoryService(GestureCategoryMapper gestureCategoryMapper) {
        this.gestureCategoryMapper = gestureCategoryMapper;
    }

    /**
     * 查询全部手势分类
     *
     * @return 手势分类列表
     */
    public List<GestureCategory> listAll() {
        return gestureCategoryMapper.selectAll();
    }
}
