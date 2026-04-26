package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Python 服务 raw 样本扫描响应。
 */
@Getter
@Setter
public class PythonRawSampleListResponse {

    /**
     * raw dataset 根目录。
     */
    private String rootDir;

    /**
     * 样本总数。
     */
    private Integer total;

    /**
     * 样本列表。
     */
    private List<PythonRawSampleItem> items;
}
