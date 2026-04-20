package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手势实体类
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GestureItem {

    /**
     * 手势ID
     */
    private Long id;

    /**
     * 所属分类ID
     */
    private Long categoryId;

    /**
     * 手势中文名
     */
    private String chineseName;

    /**
     * 模型标签
     */
    private String label;
}
