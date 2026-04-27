package com.yezhen.hearbridge.backend.interceptor;

import com.yezhen.hearbridge.backend.context.AdminAuthContext;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端认证拦截器。
 *
 * 第一版策略：
 * 1. 管理端危险操作必须登录；
 * 2. 手机端仍可读取公开分类、资源和搜索结果；
 * 3. Python 服务仍可读取当前发布模型版本。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    /**
     * 管理端认证 Service。
     */
    private final AdminAuthService adminAuthService;

    /**
     * 构造注入。
     *
     * @param adminAuthService 管理端认证 Service
     */
    public AdminAuthInterceptor(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 请求进入 Controller 前校验管理员登录态。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  handler
     * @return 是否放行
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        if (isPreflight(request) || isPublicApi(request)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        String token = adminAuthService.extractToken(authorization);

        try {
            AdminUserInfo userInfo = adminAuthService.getUserInfoByToken(token);
            AdminAuthContext.setCurrentUser(userInfo);
            return true;
        } catch (IllegalArgumentException exception) {
            writeUnauthorized(response, request.getRequestURI(), exception.getMessage());
            return false;
        }
    }

    /**
     * 请求完成后清理 ThreadLocal。
     *
     * @param request  请求
     * @param response 响应
     * @param handler  handler
     * @param ex       异常
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        AdminAuthContext.clear();
    }

    /**
     * 是否为预检请求。
     *
     * @param request 请求
     * @return 是否预检请求
     */
    private boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    /**
     * 是否为免登录接口。
     *
     * @param request 请求
     * @return 是否免登录
     */
    private boolean isPublicApi(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("/admin/auth/login".equals(path)) {
            return true;
        }

        if ("/health".equals(path)) {
            return true;
        }

        if (path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || "/swagger-ui.html".equals(path)) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) && "/sign/categories".equals(path)) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) && "/sign/resources".equals(path)) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) && "/sign/search".equals(path)) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) && "/sign/model-versions/published".equals(path)) {
            return true;
        }

        return false;
    }

    /**
     * 写 401 响应。
     *
     * @param response 响应
     * @param path     请求路径
     * @param message  错误消息
     * @throws Exception 写出异常
     */
    private void writeUnauthorized(
            HttpServletResponse response,
            String path,
            String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String safeMessage = message == null || message.isBlank()
                ? "管理员未登录或登录已过期"
                : message;

        response.getWriter().write("""
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "%s",
                  "path": "%s"
                }
                """.formatted(escapeJson(safeMessage), escapeJson(path)));
    }

    /**
     * 简单 JSON 转义。
     *
     * @param value 原始字符串
     * @return 转义后字符串
     */
    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
