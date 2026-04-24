package com.yezhen.hearbridge.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 手语资源实体。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignResource {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 资源编码。
     */
    private String code;

    /**
     * 中文名称。
     */
    private String nameZh;

    /**
     * 分类编码。
     */
    private String categoryCode;

    /**
     * SigML 在 MinIO 中的相对路径。
     */
    private String sigmlObjectKey;

    /**
     * 资源图片在 MinIO 中的相对路径。
     */
    private String coverObjectKey;

    /**
     * SigML 完整访问地址。
     */
    private String sigmlUrl;

    /**
     * 资源图片完整访问地址。
     */
    private String coverUrl;
}
