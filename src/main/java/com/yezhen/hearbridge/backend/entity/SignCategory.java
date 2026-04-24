package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手语资源分类实体。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignCategory {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 分类编码。
     */
    private String code;

    /**
     * 分类中文名。
     */
    private String nameZh;

    /**
     * 分类图片在 MinIO 中的相对路径。
     */
    private String coverObjectKey;

    /**
     * 分类图片完整访问地址。
     */
    private String coverUrl;
}
