package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.context.AdminAuthContext;
import com.yezhen.hearbridge.backend.dto.AdminLoginRequest;
import com.yezhen.hearbridge.backend.dto.AdminLoginResult;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.service.AdminAuthService;
import org.springframework.web.bind.annotation.*;
import com.yezhen.hearbridge.backend.dto.AdminChangePasswordRequest;
import java.util.Map;

/**
 * 管理端认证 Controller。
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    /**
     * 管理端认证 Service。
     */
    private final AdminAuthService adminAuthService;

    /**
     * 构造注入。
     *
     * @param adminAuthService 管理端认证 Service
     */
    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * 管理员登录。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public AdminLoginResult login(@RequestBody AdminLoginRequest request) {
        return adminAuthService.login(request);
    }

    /**
     * 获取当前管理员信息。
     *
     * @return 当前管理员信息
     */
    @GetMapping("/me")
    public AdminUserInfo me() {
        AdminUserInfo userInfo = AdminAuthContext.getCurrentUser();
        if (userInfo == null) {
            throw new IllegalArgumentException("管理员未登录");
        }
        return userInfo;
    }

    /**
     * 退出登录。
     *
     * @param authorization Authorization 请求头
     */
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = adminAuthService.extractToken(authorization);
        adminAuthService.logout(token);
    }

    /**
     * 修改当前管理员密码。
     *
     * 修改成功后当前 token 会失效，前端需要重新登录。
     *
     * @param request       修改密码请求
     * @param authorization Authorization 请求头
     * @return 修改结果
     */
    @PutMapping("/password")
    public Map<String, String> changePassword(
            @RequestBody AdminChangePasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = adminAuthService.extractToken(authorization);
        adminAuthService.changePassword(request, token);

        return Map.of("message", "密码修改成功，请重新登录");
    }
}
