package com.yezhen.hearbridge.backend.util;

import com.yezhen.hearbridge.backend.dto.PageResult;

import java.util.Collections;
import java.util.List;

/**
 * 分页工具类。
 *
 * 当前阶段说明：
 * 1. 项目暂未接入分页插件。
 * 2. 先在 Service 层基于完整列表做安全切片。
 * 3. 后续数据量上来后，可将分页下沉到 Mapper / SQL 的 limit offset。
 */
public final class PageUtils {

    /**
     * 默认页码。
     */
    private static final int DEFAULT_PAGE_NO = 1;

    /**
     * 默认每页数量。
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页数量。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 工具类禁止实例化。
     */
    private PageUtils() {
    }

    /**
     * 将完整列表切成分页结果。
     *
     * @param source   完整列表
     * @param pageNo   当前页码
     * @param pageSize 每页数量
     * @param <T>      元素类型
     * @return 分页结果
     */
    public static <T> PageResult<T> paginate(List<T> source, Integer pageNo, Integer pageSize) {
        List<T> safeSource = source == null ? Collections.emptyList() : source;
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int total = safeSource.size();

        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0, safePageNo, safePageSize);
        }

        int fromIndex = (safePageNo - 1) * safePageSize;
        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), total, safePageNo, safePageSize);
        }

        int toIndex = Math.min(fromIndex + safePageSize, total);
        return PageResult.of(safeSource.subList(fromIndex, toIndex), total, safePageNo, safePageSize);
    }

    /**
     * 规范化页码。
     *
     * @param pageNo 原始页码
     * @return 安全页码
     */
    public static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    /**
     * 规范化每页数量。
     *
     * @param pageSize 原始每页数量
     * @return 安全每页数量
     */
    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
