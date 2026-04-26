package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型版本 Mapper。
 */
@Mapper
public interface SignModelVersionMapper {

    /**
     * 查询全部模型版本。
     *
     * @return 模型版本列表
     */
    List<SignModelVersion> selectAll();

    /**
     * 根据 ID 查询模型版本。
     *
     * @param id 模型版本 ID
     * @return 模型版本
     */
    SignModelVersion selectById(@Param("id") Long id);

    /**
     * 根据训练运行名称查询模型版本。
     *
     * @param runName 训练运行名称
     * @return 模型版本
     */
    SignModelVersion selectByRunName(@Param("runName") String runName);

    /**
     * 查询当前发布版本。
     *
     * @return 当前发布版本
     */
    SignModelVersion selectPublished();

    /**
     * 新增模型版本。
     *
     * @param version 模型版本
     * @return 影响行数
     */
    int insert(SignModelVersion version);

    /**
     * 清除当前发布版本状态。
     *
     * @return 影响行数
     */
    int clearPublished();

    /**
     * 发布指定模型版本。
     *
     * @param id 模型版本 ID
     * @return 影响行数
     */
    int publishById(@Param("id") Long id);
}
