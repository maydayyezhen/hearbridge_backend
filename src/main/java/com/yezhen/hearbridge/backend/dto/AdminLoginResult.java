package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 管理端登录结果。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResult {

    /**
     * 登录 token。
     */
    private String token;

    /**
     * 当前用户信息。
     */
    private AdminUserInfo user;
}
