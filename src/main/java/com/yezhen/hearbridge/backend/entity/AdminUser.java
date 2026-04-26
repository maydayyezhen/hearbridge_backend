package com.yezhen.hearbridge.backend.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端管理员用户实体。
 */
@Getter
@Setter
public class AdminUser {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 管理员用户名。
     */
    private String username;

    /**
     * BCrypt 密码哈希。
     */
    private String passwordHash;

    /**
     * 管理员昵称。
     */
    private String nickname;

    /**
     * 状态：ENABLED、DISABLED。
     */
    private String status;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
