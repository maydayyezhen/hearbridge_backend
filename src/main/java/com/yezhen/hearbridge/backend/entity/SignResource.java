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
     * 统一编码，建议使用去后缀后的文件名。
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
     * sigml 在 MinIO 中的相对路径。
     */
    private String sigmlObjectKey;

    /**
     * 资源图片在 MinIO 中的相对路径。
     */
    private String coverObjectKey;
}
