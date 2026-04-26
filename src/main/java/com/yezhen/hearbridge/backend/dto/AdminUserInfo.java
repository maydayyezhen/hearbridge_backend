package com.yezhen.hearbridge.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 管理端当前用户信息。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserInfo {

    /**
     * 管理员 ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;
}
