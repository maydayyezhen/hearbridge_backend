package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.AdminLoginRequest;
import com.yezhen.hearbridge.backend.dto.AdminLoginResult;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.entity.AdminUser;
import com.yezhen.hearbridge.backend.mapper.AdminUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 管理端认证 Service 白盒测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    /**
     * 管理员 Mapper Mock。
     */
    @Mock
    private AdminUserMapper adminUserMapper;

    /**
     * Redis Mock。
     */
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis ValueOperations Mock。
     */
    @Mock
    private ValueOperations<String, String> valueOperations;

    /**
     * 密码编码器。
     */
    private PasswordEncoder passwordEncoder;

    /**
     * 被测试 Service。
     */
    private AdminAuthService adminAuthService;

    /**
     * 初始化测试对象。
     */
    /**
     * 初始化测试对象。
     */
    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        adminAuthService = new AdminAuthService(
                adminUserMapper,
                stringRedisTemplate,
                passwordEncoder,
                "hearbridge:admin:login:",
                7
        );
    }

    @Test
    void login_shouldReturnToken_whenUsernameAndPasswordValid() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        AdminUser adminUser = buildAdminUser();
        adminUser.setPasswordHash(passwordEncoder.encode("admin123456"));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        when(adminUserMapper.selectByUsername("admin")).thenReturn(adminUser);

        AdminLoginResult result = adminAuthService.login(request);

        assertNotNull(result.getToken());
        assertFalse(result.getToken().isBlank());
        assertEquals(1L, result.getUser().getId());
        assertEquals("admin", result.getUser().getUsername());

        verify(valueOperations).set(
                startsWith("hearbridge:admin:login:"),
                eq("1"),
                eq(Duration.ofDays(7))
        );
    }

    /**
     * 测试：密码错误时登录失败。
     */
    @Test
    void login_shouldThrowException_whenPasswordInvalid() {
        AdminUser adminUser = buildAdminUser();
        adminUser.setPasswordHash(passwordEncoder.encode("admin123456"));

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        when(adminUserMapper.selectByUsername("admin")).thenReturn(adminUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminAuthService.login(request)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    /**
     * 测试：根据 token 获取当前管理员成功。
     */
    @Test
    void getUserInfoByToken_shouldReturnUserInfo_whenTokenValid() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        AdminUser adminUser = buildAdminUser();

        when(valueOperations.get("hearbridge:admin:login:token123")).thenReturn("1");
        when(adminUserMapper.selectById(1L)).thenReturn(adminUser);

        AdminUserInfo result = adminAuthService.getUserInfoByToken("token123");

        assertEquals(1L, result.getId());
        assertEquals("admin", result.getUsername());
        assertEquals("管理员", result.getNickname());
    }

    /**
     * 测试：token 过期时抛出异常。
     */
    @Test
    void getUserInfoByToken_shouldThrowException_whenTokenExpired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get("hearbridge:admin:login:expired")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminAuthService.getUserInfoByToken("expired")
        );

        assertEquals("登录已过期，请重新登录", exception.getMessage());
    }

    /**
     * 测试：退出登录时删除 Redis token。
     */
    @Test
    void logout_shouldDeleteRedisToken() {
        adminAuthService.logout("token123");

        verify(stringRedisTemplate).delete("hearbridge:admin:login:token123");
    }

    /**
     * 构造管理员用户。
     *
     * @return 管理员用户
     */
    private AdminUser buildAdminUser() {
        AdminUser adminUser = new AdminUser();

        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setNickname("管理员");
        adminUser.setStatus("ENABLED");

        return adminUser;
    }
}
