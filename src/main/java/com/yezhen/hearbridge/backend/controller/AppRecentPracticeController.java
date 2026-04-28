package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.AppRecentPracticeDto;
import com.yezhen.hearbridge.backend.dto.AppRecentPracticeUpdateRequest;
import com.yezhen.hearbridge.backend.service.AppRecentPracticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 最近练习接口。
 */
@RestController
@RequestMapping("/app/user/recent-practice")
public class AppRecentPracticeController {

    /**
     * 最近练习服务。
     */
    private final AppRecentPracticeService appRecentPracticeService;

    public AppRecentPracticeController(AppRecentPracticeService appRecentPracticeService) {
        this.appRecentPracticeService = appRecentPracticeService;
    }

    /**
     * 获取当前用户最近练习。
     */
    @GetMapping
    public AppRecentPracticeDto getRecentPractice(@RequestHeader(value = "token", required = false) String token) {
        return appRecentPracticeService.getRecentPractice(token);
    }

    /**
     * 更新当前用户最近练习。
     */
    @PutMapping
    public AppRecentPracticeDto updateRecentPractice(@RequestHeader(value = "token", required = false) String token,
                                                     @RequestBody AppRecentPracticeUpdateRequest request) {
        return appRecentPracticeService.updateRecentPractice(token, request);
    }
}
