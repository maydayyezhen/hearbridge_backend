package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.AppAuthResponse;
import com.yezhen.hearbridge.backend.dto.AppLoginRequest;
import com.yezhen.hearbridge.backend.dto.AppRegisterRequest;
import com.yezhen.hearbridge.backend.dto.AppUserProfile;
import com.yezhen.hearbridge.backend.service.AppAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 手机端认证 Controller。
 */
@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    /**
     * 手机端认证服务。
     */
    private final AppAuthService appAuthService;

    /**
     * 构造注入。
     *
     * @param appAuthService 手机端认证服务
     */
    public AppAuthController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    /**
     * 注册。
     *
     * @param request 注册请求
     * @return 认证结果
     */
    @PostMapping("/register")
    public AppAuthResponse register(@RequestBody AppRegisterRequest request) {
        return appAuthService.register(request);
    }

    /**
     * 登录。
     *
     * @param request 登录请求
     * @return 认证结果
     */
    @PostMapping("/login")
    public AppAuthResponse login(@RequestBody AppLoginRequest request) {
        return appAuthService.login(request);
    }

    /**
     * 查询当前用户。
     *
     * 当前用户由 AuthInterceptor 放入 AppAuthContext。
     *
     * @return 当前用户资料
     */
    @GetMapping("/me")
    public AppUserProfile me() {
        return appAuthService.getCurrentUser();
    }

    /**
     * 退出登录。
     *
     * 当前版本退出登录仍由前端清理本地 token。
     * 如果后续需要服务端删除当前 token，可在 AuthInterceptor 中保存 currentToken。
     */
    @PostMapping("/logout")
    public void logout() {
    }
}
