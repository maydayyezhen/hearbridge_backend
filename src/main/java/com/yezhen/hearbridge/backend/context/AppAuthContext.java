package com.yezhen.hearbridge.backend.context;

import com.yezhen.hearbridge.backend.entity.AppUser;

/**
 * 手机端用户认证上下文。
 *
 * 用 ThreadLocal 保存当前请求中的 App 用户信息。
 */
public final class AppAuthContext {

    /**
     * 当前请求的 App 用户。
     */
    private static final ThreadLocal<AppUser> CURRENT_USER = new ThreadLocal<>();

    /**
     * 工具类禁止实例化。
     */
    private AppAuthContext() {
    }

    /**
     * 设置当前 App 用户。
     *
     * @param user 当前 App 用户
     */
    public static void setCurrentUser(AppUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前 App 用户。
     *
     * @return 当前 App 用户
     */
    public static AppUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 清理当前请求上下文。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
