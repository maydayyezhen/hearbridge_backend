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
     * 根据主键 ID 查询单个资源。
     *
     * @param id 资源主键 ID
     * @return 资源详情
     */
    SignResource selectById(@Param("id") Long id);

    /**
     * 根据资源编码查询单个资源。
     *
     * @param code 资源编码
     * @return 资源详情
     */
    SignResource selectByCode(@Param("code") String code);

    /**
     * 统计某个分类编码下的资源数量。
     *
     * @param categoryCode 分类编码
     * @return 资源数量
     */
    int countByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 新增资源。
     *
     * @param resource 资源信息
     * @return 影响行数
     */
    int insert(SignResource resource);

    /**
     * 根据主键 ID 更新资源。
     *
     * @param resource 资源信息
     * @return 影响行数
     */
    int updateById(SignResource resource);

    /**
     * 根据主键 ID 删除资源。
     *
     * @param id 资源主键 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
