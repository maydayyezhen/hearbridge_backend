package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.SignResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 手语资源 Mapper。
 */
@Mapper
public interface SignResourceMapper {

    /**
     * 查询全部资源。
     *
     * @return 资源列表
     */
    List<SignResource> selectAll();

    /**
     * 根据分类编码查询资源列表。
     *
     * @param categoryCode 分类编码
     * @return 资源列表
     */
    List<SignResource> selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 根据资源编码查询单个资源。
     *
     * @param code 资源编码
     * @return 资源详情
     */
    SignResource selectByCode(@Param("code") String code);
}
