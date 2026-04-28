package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.AppRecentPracticeDto;
import com.yezhen.hearbridge.backend.dto.AppRecentPracticeUpdateRequest;
import com.yezhen.hearbridge.backend.entity.AppUser;
import com.yezhen.hearbridge.backend.mapper.AppUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

/**
 * App 用户最近练习服务。
 */
@Service
public class AppRecentPracticeService {

    /** App 用户 Mapper。 */
    private final AppUserMapper appUserMapper;

    /** Redis token 存储。 */
    private final StringRedisTemplate redisTemplate;

    /** App 登录 token 前缀。 */
    private final String tokenPrefix;

    /** App 登录 token 续期天数。 */
    private final long tokenExpireDays;

    public AppRecentPracticeService(
            AppUserMapper appUserMapper,
            StringRedisTemplate redisTemplate,
            @Value("${hearbridge.auth.token-prefix:hearbridge:app:login:}") String tokenPrefix,
            @Value("${hearbridge.auth.token-expire-days:7}") long tokenExpireDays) {
        this.appUserMapper = appUserMapper;
        this.redisTemplate = redisTemplate;
        this.tokenPrefix = tokenPrefix;
        this.tokenExpireDays = tokenExpireDays;
    }

    /**
     * 获取当前用户最近练习。
     *
     * @param token 登录 token
     * @return 最近练习
     */
    public AppRecentPracticeDto getRecentPractice(String token) {
        AppUser user = requireUser(token);
        return toRecentPractice(user);
    }

    /**
     * 更新当前用户最近练习。
     *
     * @param token 登录 token
     * @param request 最近练习更新请求
     * @return 更新后的最近练习
     */
    public AppRecentPracticeDto updateRecentPractice(String token, AppRecentPracticeUpdateRequest request) {
        AppUser user = requireUser(token);

        Long resourceId = request == null ? null : request.getResourceId();
        String resourceCode = trim(request == null ? null : request.getResourceCode());
        String chineseName = trim(request == null ? null : request.getChineseName());
        String sigmlUrl = trim(request == null ? null : request.getSigmlUrl());
        String coverUrl = trim(request == null ? null : request.getCoverUrl());

        if (!StringUtils.hasText(resourceCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource code cannot be empty");
        }

        if (!StringUtils.hasText(chineseName)) {
            chineseName = resourceCode;
        }

        appUserMapper.updateRecentPracticeById(
                user.getId(),
                resourceId,
                resourceCode,
                chineseName,
                sigmlUrl,
                coverUrl
        );

        return new AppRecentPracticeDto(
                resourceId,
                resourceCode,
                chineseName,
                sigmlUrl,
                coverUrl
        );
    }

    /**
     * 根据 token 获取当前 App 用户。
     *
     * @param token 登录 token
     * @return 当前 App 用户
     */
    private AppUser requireUser(String token) {
        String normalizedToken = normalizeToken(token);
        if (!StringUtils.hasText(normalizedToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        }

        String userIdValue = redisTemplate.opsForValue().get(tokenPrefix + normalizedToken);
        if (!StringUtils.hasText(userIdValue)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login expired");
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdValue);
        } catch (NumberFormatException ex) {
            redisTemplate.delete(tokenPrefix + normalizedToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login expired");
        }

        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            redisTemplate.delete(tokenPrefix + normalizedToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        redisTemplate.expire(tokenPrefix + normalizedToken, tokenExpireDays, TimeUnit.DAYS);
        return user;
    }

    /**
     * 转换最近练习 DTO。
     *
     * @param user 当前用户
     * @return 最近练习 DTO
     */
    private AppRecentPracticeDto toRecentPractice(AppUser user) {
        return new AppRecentPracticeDto(
                user.getRecentPracticeResourceId(),
                safeString(user.getRecentPracticeResourceCode()),
                safeString(user.getRecentPracticeChineseName()),
                safeString(user.getRecentPracticeSigmlUrl()),
                safeString(user.getRecentPracticeCoverUrl())
        );
    }

    /**
     * 解析 Bearer token。
     *
     * @param token 原始 token
     * @return 规范 token
     */
    private String normalizeToken(String token) {
        String normalized = trim(token);
        if (StringUtils.hasText(normalized) && normalized.startsWith("Bearer ")) {
            return normalized.substring(7).trim();
        }
        return normalized;
    }

    /**
     * 安全字符串。
     *
     * @param value 原始值
     * @return 非空字符串
     */
    private String safeString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 去除字符串首尾空白。
     *
     * @param value 原始字符串
     * @return 安全字符串
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
