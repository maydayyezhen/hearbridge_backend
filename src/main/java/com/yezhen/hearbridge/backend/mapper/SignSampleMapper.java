package com.yezhen.hearbridge.backend.mapper;

import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignSample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 手势样本 Mapper。
 */
@Mapper
public interface SignSampleMapper {

    /**
     * 分页查询样本列表。
     *
     * @param query    查询条件
     * @param offset   分页偏移量
     * @param pageSize 每页数量
     * @return 样本列表
     */
    List<SignSample> selectPage(
            @Param("query") SignSampleQuery query,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计分页查询总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    Long countPage(@Param("query") SignSampleQuery query);

    /**
     * 根据 ID 查询样本。
     *
     * @param id 样本 ID
     * @return 样本信息
     */
    SignSample selectById(@Param("id") Long id);

    /**
     * 查询样本统计信息。
     *
     * @return 样本统计信息
     */
    SignSampleSummary selectSummary();

    /**
     * 更新样本质量状态。
     *
     * @param id             样本 ID
     * @param qualityStatus  质量状态
     * @param qualityMessage 质量说明
     * @return 影响行数
     */
    int updateQuality(
            @Param("id") Long id,
            @Param("qualityStatus") String qualityStatus,
            @Param("qualityMessage") String qualityMessage
    );

    /**
     * 软删除样本。
     *
     * @param id 样本 ID
     * @return 影响行数
     */
    int softDeleteById(@Param("id") Long id);
}
