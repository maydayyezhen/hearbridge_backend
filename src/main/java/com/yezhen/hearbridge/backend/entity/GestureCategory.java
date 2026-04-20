package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手势分类实体类
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GestureCategory {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类logo
     */
    private String logo;
}
