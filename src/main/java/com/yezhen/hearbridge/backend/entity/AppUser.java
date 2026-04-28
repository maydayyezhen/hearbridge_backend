package com.yezhen.hearbridge.backend.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppUser {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 密码哈希。
     */
    private String passwordHash;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 头像地址或对象 Key。
     */
    private String avatarUrl;

    /**
     * 最近练习资源 ID。
     */
    private Long recentPracticeResourceId;

    /**
     * 最近练习资源编码。
     */
    private String recentPracticeResourceCode;

    /**
     * 最近练习中文名。
     */
    private String recentPracticeChineseName;

    /**
     * 最近练习 SiGML 地址。
     */
    private String recentPracticeSigmlUrl;

    /**
     * 最近练习封面地址。
     */
    private String recentPracticeCoverUrl;
}
