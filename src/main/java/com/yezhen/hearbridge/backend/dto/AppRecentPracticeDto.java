package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * App 用户最近练习展示 DTO。
 */
@Getter
@AllArgsConstructor
public class AppRecentPracticeDto {

    /**
     * 最近练习资源 ID。
     */
    private Long resourceId;

    /**
     * 最近练习资源编码。
     */
    private String resourceCode;

    /**
     * 最近练习中文名。
     */
    private String chineseName;

    /**
     * 最近练习 SiGML 地址。
     */
    private String sigmlUrl;

    /**
     * 最近练习封面地址。
     */
    private String coverUrl;
}
