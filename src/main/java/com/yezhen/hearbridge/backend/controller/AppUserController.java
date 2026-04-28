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

/**
 * 手机端用户 Controller。
 */
@RestController
@RequestMapping("/app/user")
public class AppUserController {

    /**
     * 手机端认证服务。
     */
    private final AppAuthService appAuthService;

    /**
     * 构造注入。
     *
     * @param appAuthService 手机端认证服务
     */
    public AppUserController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    /**
     * 更新当前用户资料。
     *
     * @param request 更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/profile")
    public AppUserProfile updateProfile(@RequestBody AppProfileUpdateRequest request) {
        return appAuthService.updateProfile(request);
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 修改密码请求
     * @return 提示信息
     */
    @PutMapping("/password")
    public Map<String, String> changePassword(@RequestBody Map<String, String> request) {
        appAuthService.changePassword(request);
        return Map.of("message", "密码修改成功，请重新登录");
    }

    /**
     * 上传当前用户头像。
     *
     * @param fileName 文件名
     * @param request HTTP 请求
     * @return 更新后的用户资料
     * @throws IOException IO 异常
     */
    @PostMapping("/avatar")
    public AppUserProfile uploadAvatar(@RequestHeader(value = "X-Filename", required = false) String fileName,
                                       HttpServletRequest request) throws IOException {
        String decodedFileName = fileName == null || fileName.isBlank()
                ? "avatar.jpg"
                : URLDecoder.decode(fileName.trim(), StandardCharsets.UTF_8);

        return appAuthService.uploadAvatar(
                request.getInputStream(),
                request.getContentLengthLong(),
                decodedFileName,
                request.getContentType()
        );
    }
}
