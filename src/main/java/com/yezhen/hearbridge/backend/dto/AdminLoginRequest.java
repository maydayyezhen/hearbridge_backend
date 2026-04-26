package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 管理端登录请求。
 */
@Getter
@Setter
public class AdminLoginRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码。
     */
    private String password;
}
