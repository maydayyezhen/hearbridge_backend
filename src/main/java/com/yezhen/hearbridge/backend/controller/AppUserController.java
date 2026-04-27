package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.AppProfileUpdateRequest;
import com.yezhen.hearbridge.backend.dto.AppUserProfile;
import com.yezhen.hearbridge.backend.service.AppAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/app/user")
public class AppUserController {

    private final AppAuthService appAuthService;

    public AppUserController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    @PutMapping("/profile")
    public AppUserProfile updateProfile(@RequestHeader(value = "token", required = false) String token,
                                        @RequestBody AppProfileUpdateRequest request) {
        return appAuthService.updateProfile(token, request);
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@RequestHeader(value = "token", required = false) String token,
                                              @RequestBody Map<String, String> request) {
        appAuthService.changePassword(token, request);
        return Map.of("message", "密码修改成功，请重新登录");
    }

    @PostMapping("/avatar")
    public AppUserProfile uploadAvatar(@RequestHeader(value = "token", required = false) String token,
                                       @RequestHeader(value = "X-Filename", required = false) String fileName,
                                       HttpServletRequest request) throws IOException {
        String decodedFileName = fileName == null || fileName.isBlank()
                ? "avatar.jpg"
                : URLDecoder.decode(fileName.trim(), StandardCharsets.UTF_8);
        return appAuthService.uploadAvatar(
                token,
                request.getInputStream(),
                request.getContentLengthLong(),
                decodedFileName,
                request.getContentType()
        );
    }
}
