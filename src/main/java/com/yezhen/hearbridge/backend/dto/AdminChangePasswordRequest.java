package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 管理员修改密码请求。
 */
@Getter
@Setter
public class AdminChangePasswordRequest {

    /**
     * 原密码。
     */
    private String oldPassword;

    /**
     * 新密码。
     */
    private String newPassword;

    /**
     * 确认新密码。
     */
    private String confirmPassword;
}
