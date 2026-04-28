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
     * 查询分类总数。
     *
     * @return 分类总数
     */
    int countAll();

    /**
     * 分页查询分类。
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 当前页分类列表
     */
    List<SignCategory> selectPage(
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 搜索分类总数。
     *
     * @param keyword 搜索关键词
     * @return 匹配分类总数
     */
    int countByKeyword(@Param("keyword") String keyword);

    /**
     * 分页搜索分类。
     *
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 当前页分类列表
     */
    List<SignCategory> searchPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 根据主键 ID 查询单个分类。
     *
     * @param id 分类主键 ID
     * @return 分类信息
     */
    SignCategory selectById(@Param("id") Long id);

    /**
     * 根据分类编码查询单个分类。
     *
     * @param code 分类编码
     * @return 分类信息
     */
    SignCategory selectByCode(@Param("code") String code);

    /**
     * 新增分类。
     *
     * @param category 分类信息
     * @return 影响行数
     */
    int insert(SignCategory category);

    /**
     * 根据主键 ID 更新分类。
     *
     * @param category 分类信息
     * @return 影响行数
     */
    int updateById(SignCategory category);

    /**
     * 根据主键 ID 删除分类。
     *
     * @param id 分类主键 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
