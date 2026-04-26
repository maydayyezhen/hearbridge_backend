package com.yezhen.hearbridge.backend.dto;

import com.yezhen.hearbridge.backend.entity.SignSample;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 手势样本分页查询结果。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignSamplePageResult {

    /**
     * 样本列表。
     */
    private List<SignSample> items;

    /**
     * 总数量。
     */
    private Long total;

    /**
     * 当前页码。
     */
    private Integer page;

    /**
     * 每页数量。
     */
    private Integer pageSize;
}
