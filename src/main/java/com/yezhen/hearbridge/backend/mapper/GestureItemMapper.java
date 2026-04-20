package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.GestureItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 手势 Mapper
 */
@Mapper
public interface GestureItemMapper {

    /**
     * 根据分类ID查询手势列表
     *
     * @param categoryId 分类ID
     * @return 手势列表
     */
    List<GestureItem> selectByCategoryId(@Param("categoryId") Long categoryId);
}
