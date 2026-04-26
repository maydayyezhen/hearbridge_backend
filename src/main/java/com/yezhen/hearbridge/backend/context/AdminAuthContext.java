package com.yezhen.hearbridge.backend.context;

import com.yezhen.hearbridge.backend.dto.AdminUserInfo;

/**
 * 管理端登录上下文。
 *
 * 用 ThreadLocal 保存当前请求中的管理员信息。
 */
public final class AdminAuthContext {

    /**
     * 当前管理员上下文。
     */
    private static final ThreadLocal<AdminUserInfo> CURRENT_USER = new ThreadLocal<>();

    private AdminAuthContext() {
    }

    /**
     * 设置当前管理员。
     *
     * @param userInfo 管理员信息
     */
    public static void setCurrentUser(AdminUserInfo userInfo) {
        CURRENT_USER.set(userInfo);
    }

    /**
     * 获取当前管理员。
     *
     * @return 管理员信息
     */
    public static AdminUserInfo getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 清理上下文。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
