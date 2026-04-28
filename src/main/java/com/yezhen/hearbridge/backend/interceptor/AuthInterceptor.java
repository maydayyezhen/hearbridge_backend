package com.yezhen.hearbridge.backend.interceptor;

import com.yezhen.hearbridge.backend.context.AdminAuthContext;
import com.yezhen.hearbridge.backend.context.AppAuthContext;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.entity.AppUser;
import com.yezhen.hearbridge.backend.service.AdminAuthService;
import com.yezhen.hearbridge.backend.service.AppAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

/**
 * 统一认证拦截器。
 *
 * 认证规则：
 * 1. PUBLIC：公开接口，直接放行。
 * 2. APP：手机端用户接口，校验 app token。
 * 3. ADMIN：管理端接口，校验 admin token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /**
     * 手机端认证服务。
     */
    private final AppAuthService appAuthService;

    /**
     * 管理端认证服务。
     */
    private final AdminAuthService adminAuthService;

    /**
     * 构造注入。
     *
     * @param appAuthService 手机端认证服务
     * @param adminAuthService 管理端认证服务
     */
    public AuthInterceptor(AppAuthService appAuthService, AdminAuthService adminAuthService) {
        this.appAuthService = appAuthService;
        this.adminAuthService = adminAuthService;
    }

    /**
     * 请求进入 Controller 前进行统一认证。
     *
     * @param request 请求
     * @param response 响应
     * @param handler handler
     * @return 是否放行
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();
        AuthType authType = resolveAuthType(method, path);

        if (authType == AuthType.PUBLIC) {
            log.info("Auth public pass: method={}, path={}", method, path);
            return true;
        }

        String token = resolveToken(request);

        log.info(
                "Auth check: method={}, path={}, authType={}, tokenLength={}",
                method,
                path,
                authType,
                token.length()
        );

        try {
            if (authType == AuthType.APP) {
                AppUser appUser = appAuthService.requireUser(token);
                AppAuthContext.setCurrentUser(appUser);

                log.info(
                        "App auth passed: method={}, path={}, userId={}",
                        method,
                        path,
                        appUser.getId()
                );
                return true;
            }

            if (authType == AuthType.ADMIN) {
                AdminUserInfo adminUser = adminAuthService.getUserInfoByToken(token);
                AdminAuthContext.setCurrentUser(adminUser);

                log.info(
                        "Admin auth passed: method={}, path={}, adminUserId={}",
                        method,
                        path,
                        adminUser.getId()
                );
                return true;
            }

            writeUnauthorized(response, path, "未授权访问");
            return false;
        } catch (ResponseStatusException exception) {
            log.warn(
                    "App auth failed: method={}, path={}, reason={}",
                    method,
                    path,
                    exception.getReason()
            );
            writeUnauthorized(response, path, exception.getReason());
            return false;
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "Admin auth failed: method={}, path={}, reason={}",
                    method,
                    path,
                    exception.getMessage()
            );
            writeUnauthorized(response, path, exception.getMessage());
            return false;
        }
    }

    /**
     * 请求完成后清理认证上下文。
     *
     * @param request 请求
     * @param response 响应
     * @param handler handler
     * @param ex 异常
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        AppAuthContext.clear();
        AdminAuthContext.clear();
    }

    /**
     * 根据请求方法和路径解析认证类型。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 认证类型
     */
    private AuthType resolveAuthType(String method, String path) {
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return AuthType.PUBLIC;
        }

        if ("/health".equals(path)) {
            return AuthType.PUBLIC;
        }

        if (path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || "/swagger-ui.html".equals(path)) {
            return AuthType.PUBLIC;
        }

        if ("/admin/auth/login".equals(path)) {
            return AuthType.PUBLIC;
        }

        if ("/app/auth/login".equals(path) || "/app/auth/register".equals(path)) {
            return AuthType.PUBLIC;
        }

        if (isPublicSignGetApi(method, path)) {
            return AuthType.PUBLIC;
        }

        if (path.startsWith("/app/")) {
            return AuthType.APP;
        }

        return AuthType.ADMIN;
    }

    /**
     * 判断是否为公开手语查询接口。
     *
     * @param method 请求方法
     * @param path 请求路径
     * @return 是否公开
     */
    private boolean isPublicSignGetApi(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        return "/sign/categories".equals(path)
                || "/sign/categories/page".equals(path)
                || "/sign/resources".equals(path)
                || "/sign/resources/page".equals(path)
                || "/sign/search".equals(path)
                || "/sign/model-versions/published".equals(path);
    }

    /**
     * 从请求中解析 token。
     *
     * 优先级：
     * 1. Authorization: Bearer xxx
     * 2. token: xxx
     *
     * @param request 请求
     * @return token
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            String trimmed = authorization.trim();
            String prefix = "Bearer ";
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }

        String legacyToken = request.getHeader("token");
        if (StringUtils.hasText(legacyToken)) {
            return legacyToken.trim();
        }

        return "";
    }

    /**
     * 写 401 响应。
     *
     * @param response 响应
     * @param path 请求路径
     * @param message 错误消息
     * @throws Exception 写出异常
     */
    private void writeUnauthorized(
            HttpServletResponse response,
            String path,
            String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String safeMessage = StringUtils.hasText(message)
                ? message
                : "未登录或登录已过期";

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
