package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.GestureCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 手势分类 Mapper
 */
@Mapper
public interface GestureCategoryMapper {

    /**
     * 查询全部手势分类
     *
     * @return 手势分类列表
     */
    List<GestureCategory> selectAll();
}
