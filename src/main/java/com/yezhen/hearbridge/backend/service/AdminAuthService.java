package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.AdminLoginRequest;
import com.yezhen.hearbridge.backend.dto.AdminLoginResult;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.entity.AdminUser;
import com.yezhen.hearbridge.backend.mapper.AdminUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * 管理端认证 Service。
 *
 * 使用 UUID token + Redis 保存登录会话。
 */
@Service
public class AdminAuthService {

    /**
     * 启用状态。
     */
    private static final String STATUS_ENABLED = "ENABLED";

    /**
     * 管理员用户 Mapper。
     */
    private final AdminUserMapper adminUserMapper;

    /**
     * Redis 操作工具。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 密码编码器。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 管理端 token Redis 前缀。
     */
    private final String adminTokenPrefix;

    /**
     * 管理端 token 过期天数。
     */
    private final Integer adminTokenExpireDays;

    /**
     * 构造注入依赖。
     *
     * @param adminUserMapper     管理员用户 Mapper
     * @param stringRedisTemplate Redis 操作工具
     * @param passwordEncoder     密码编码器
     * @param adminTokenPrefix    管理端 token Redis 前缀
     * @param adminTokenExpireDays 管理端 token 过期天数
     */
    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            StringRedisTemplate stringRedisTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${hearbridge.auth.admin-token-prefix:hearbridge:admin:login:}") String adminTokenPrefix,
            @Value("${hearbridge.auth.admin-token-expire-days:7}") Integer adminTokenExpireDays) {
        this.adminUserMapper = adminUserMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenPrefix = adminTokenPrefix;
        this.adminTokenExpireDays = adminTokenExpireDays;
    }

    /**
     * 管理员登录。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    public AdminLoginResult login(AdminLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("登录请求不能为空");
        }

        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        AdminUser adminUser = adminUserMapper.selectByUsername(request.getUsername());
        if (adminUser == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!STATUS_ENABLED.equals(adminUser.getStatus())) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), adminUser.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = buildRedisKey(token);

        stringRedisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(adminUser.getId()),
                Duration.ofDays(adminTokenExpireDays)
        );

        return new AdminLoginResult(
                token,
                toUserInfo(adminUser)
        );
    }

    /**
     * 根据 token 查询当前管理员。
     *
     * @param token token
     * @return 管理员信息
     */
    public AdminUserInfo getUserInfoByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("管理员未登录");
        }

        String userIdText = stringRedisTemplate.opsForValue().get(buildRedisKey(token));
        if (!StringUtils.hasText(userIdText)) {
            throw new IllegalArgumentException("登录已过期，请重新登录");
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("登录状态异常，请重新登录");
        }

        AdminUser adminUser = adminUserMapper.selectById(userId);
        if (adminUser == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        if (!STATUS_ENABLED.equals(adminUser.getStatus())) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }

        return toUserInfo(adminUser);
    }

    /**
     * 退出登录。
     *
     * @param token token
     */
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        stringRedisTemplate.delete(buildRedisKey(token));
    }

    /**
     * 从 Authorization 请求头中提取 Bearer token。
     *
     * @param authorization Authorization 请求头
     * @return token
     */
    public String extractToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return "";
        }

        String trimmed = authorization.trim();
        String prefix = "Bearer ";

        if (!trimmed.startsWith(prefix)) {
            return "";
        }

        return trimmed.substring(prefix.length()).trim();
    }

    /**
     * 构造 Redis key。
     *
     * @param token token
     * @return Redis key
     */
    private String buildRedisKey(String token) {
        return adminTokenPrefix + token;
    }

    /**
     * 转换为前端用户信息。
     *
     * @param adminUser 管理员实体
     * @return 管理员信息
     */
    private AdminUserInfo toUserInfo(AdminUser adminUser) {
        return new AdminUserInfo(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getNickname()
        );
    }
}
