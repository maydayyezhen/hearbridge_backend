package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.AppAuthResponse;
import com.yezhen.hearbridge.backend.dto.AppLoginRequest;
import com.yezhen.hearbridge.backend.dto.AppRegisterRequest;
import com.yezhen.hearbridge.backend.dto.AppUserProfile;
import com.yezhen.hearbridge.backend.service.AppAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    private final AppAuthService appAuthService;

    public AppAuthController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    @PostMapping("/register")
    public AppAuthResponse register(@RequestBody AppRegisterRequest request) {
        return appAuthService.register(request);
    }

    @PostMapping("/login")
    public AppAuthResponse login(@RequestBody AppLoginRequest request) {
        return appAuthService.login(request);
    }

    @GetMapping("/me")
    public AppUserProfile me(@RequestHeader(value = "token", required = false) String token) {
        return appAuthService.getCurrentUser(token);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "token", required = false) String token) {
        appAuthService.logout(token);
    }
}
