package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.SignCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 手语资源分类 Mapper。
 */
@Mapper
public interface SignCategoryMapper {

    /**
     * 查询全部分类。
     *
     * @return 分类列表
     */
    List<SignCategory> selectAll();

    /**
     * 根据分类编码查询单个分类。
     *
     * @param code 分类编码
     * @return 分类信息
     */
    SignCategory selectByCode(@Param("code") String code);
}
