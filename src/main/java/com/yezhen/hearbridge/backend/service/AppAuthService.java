package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.dto.AppAuthResponse;
import com.yezhen.hearbridge.backend.dto.AppLoginRequest;
import com.yezhen.hearbridge.backend.dto.AppProfileUpdateRequest;
import com.yezhen.hearbridge.backend.dto.AppRegisterRequest;
import com.yezhen.hearbridge.backend.dto.AppUserProfile;
import com.yezhen.hearbridge.backend.entity.AppUser;
import com.yezhen.hearbridge.backend.mapper.AppUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AppAuthService {

    private final AppUserMapper appUserMapper;
    private final StringRedisTemplate redisTemplate;
    private final MinioProperties minioProperties;
    private final MinioStorageService minioStorageService;
    private final String tokenPrefix;
    private final long tokenExpireDays;

    public AppAuthService(
            AppUserMapper appUserMapper,
            StringRedisTemplate redisTemplate,
            MinioProperties minioProperties,
            MinioStorageService minioStorageService,
            @Value("${hearbridge.auth.token-prefix:hearbridge:app:login:}") String tokenPrefix,
            @Value("${hearbridge.auth.token-expire-days:7}") long tokenExpireDays) {
        this.appUserMapper = appUserMapper;
        this.redisTemplate = redisTemplate;
        this.minioProperties = minioProperties;
        this.minioStorageService = minioStorageService;
        this.tokenPrefix = tokenPrefix;
        this.tokenExpireDays = tokenExpireDays;
    }

    public AppAuthResponse register(AppRegisterRequest request) {
        String username = trim(request == null ? null : request.getUsername());
        String password = trim(request == null ? null : request.getPassword());
        String nickname = trim(request == null ? null : request.getNickname());

        validateUsername(username);
        validatePassword(password);
        if (!StringUtils.hasText(nickname)) {
            nickname = username;
        }

        if (appUserMapper.selectByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(encryptPassword(password));
        user.setNickname(nickname);
        user.setAvatarUrl("");
        appUserMapper.insert(user);

        return buildAuthResponse(user);
    }

    public AppAuthResponse login(AppLoginRequest request) {
        String username = trim(request == null ? null : request.getUsername());
        String password = trim(request == null ? null : request.getPassword());

        validateUsername(username);
        validatePassword(password);

        AppUser user = appUserMapper.selectByUsername(username);
        if (user == null || !encryptPassword(password).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return buildAuthResponse(user);
    }

    public AppUserProfile getCurrentUser(String token) {
        return toProfile(requireUser(token));
    }

    public AppUserProfile updateProfile(String token, AppProfileUpdateRequest request) {
        AppUser user = requireUser(token);
        String nickname = trim(request == null ? null : request.getNickname());
        if (!StringUtils.hasText(nickname)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname cannot be empty");
        }

        appUserMapper.updateProfileById(user.getId(), nickname, user.getAvatarUrl());
        user.setNickname(nickname);
        return toProfile(user);
    }

    public void changePassword(String token, Map<String, String> request) {
        String normalizedToken = normalizeToken(token);
        AppUser user = requireUser(normalizedToken);
        String oldPassword = trim(request == null ? null : request.get("oldPassword"));
        String newPassword = trim(request == null ? null : request.get("newPassword"));
        String confirmPassword = trim(request == null ? null : request.get("confirmPassword"));

        validatePassword(oldPassword);
        validatePassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }
        if (!encryptPassword(oldPassword).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (encryptPassword(newPassword).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }

        appUserMapper.updatePasswordById(user.getId(), encryptPassword(newPassword));
        logout(normalizedToken);
    }

    public AppUserProfile uploadAvatar(String token,
                                       InputStream inputStream,
                                       long size,
                                       String fileName,
                                       String contentType) {
        AppUser user = requireUser(token);
        String objectKey = minioStorageService.uploadAvatar(
                user.getId(),
                inputStream,
                size,
                fileName,
                contentType
        );
        appUserMapper.updateAvatarById(user.getId(), objectKey);
        user.setAvatarUrl(objectKey);
        return toProfile(user);
    }

    public void logout(String token) {
        String normalizedToken = normalizeToken(token);
        if (!StringUtils.hasText(normalizedToken)) {
            return;
        }
        redisTemplate.delete(tokenPrefix + normalizedToken);
    }

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

    private AppAuthResponse buildAuthResponse(AppUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                tokenPrefix + token,
                String.valueOf(user.getId()),
                tokenExpireDays,
                TimeUnit.DAYS
        );
        return new AppAuthResponse(token, toProfile(user));
    }

    private AppUserProfile toProfile(AppUser user) {
        return AppUserProfile.from(user, minioProperties);
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username) || username.length() < 3 || username.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username length must be between 3 and 64");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length must be between 6 and 64");
        }
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeToken(String token) {
        String normalized = trim(token);
        if (StringUtils.hasText(normalized) && normalized.startsWith("Bearer ")) {
            return normalized.substring(7).trim();
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
