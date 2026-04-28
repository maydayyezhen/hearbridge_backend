package com.yezhen.hearbridge.backend.interceptor;

/**
 * 接口认证类型。
 */
public enum AuthType {

    /**
     * 公开接口，不需要 token。
     */
    PUBLIC,

    /**
     * 手机端用户接口，需要 app token。
     */
    APP,

    /**
     * 管理端接口，需要 admin token。
     */
    ADMIN
}
