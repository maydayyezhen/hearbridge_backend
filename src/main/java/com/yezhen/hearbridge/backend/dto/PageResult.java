package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 通用分页响应模型。
 *
 * @param <T> 当前页数据类型
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页数据。
     */
    private List<T> records;

    /**
     * 总记录数。
     */
    private long total;

    /**
     * 当前页码，从 1 开始。
     */
    private int pageNo;

    /**
     * 每页数量。
     */
    private int pageSize;

    /**
     * 总页数。
     */
    private long totalPages;

    /**
     * 是否还有下一页。
     */
    private boolean hasNext;

    /**
     * 根据 records / total / pageNo / pageSize 创建分页结果。
     *
     * @param records  当前页数据
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNo, int pageSize) {
        long totalPages = pageSize <= 0 ? 0 : (long) Math.ceil((double) total / (double) pageSize);
        boolean hasNext = pageNo < totalPages;
        return new PageResult<>(records, total, pageNo, pageSize, totalPages, hasNext);
    }
}
