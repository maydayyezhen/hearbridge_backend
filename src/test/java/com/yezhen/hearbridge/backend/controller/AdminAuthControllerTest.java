package com.yezhen.hearbridge.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.context.AdminAuthContext;
import com.yezhen.hearbridge.backend.dto.AdminLoginRequest;
import com.yezhen.hearbridge.backend.dto.AdminLoginResult;
import com.yezhen.hearbridge.backend.dto.AdminUserInfo;
import com.yezhen.hearbridge.backend.service.AdminAuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端认证 Controller 黑盒测试。
 */
class AdminAuthControllerTest {

    /**
     * MockMvc。
     */
    private MockMvc mockMvc;

    /**
     * JSON 工具。
     */
    private ObjectMapper objectMapper;

    /**
     * 管理端认证 Service Mock。
     */
    private AdminAuthService adminAuthService;

    /**
     * 初始化测试对象。
     */
    @BeforeEach
    void setUp() {
        adminAuthService = Mockito.mock(AdminAuthService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminAuthController(adminAuthService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 清理上下文。
     */
    @AfterEach
    void tearDown() {
        AdminAuthContext.clear();
    }

    /**
     * 测试：登录成功。
     */
    @Test
    void login_shouldReturnTokenAndUserInfo() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        AdminLoginResult result = new AdminLoginResult(
                "token123",
                new AdminUserInfo(1L, "admin", "管理员")
        );

        Mockito.when(adminAuthService.login(any(AdminLoginRequest.class))).thenReturn(result);

        mockMvc.perform(post("/admin/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.nickname").value("管理员"));
    }

    /**
     * 测试：登录失败返回 400。
     */
    @Test
    void login_shouldReturnBadRequest_whenPasswordInvalid() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        Mockito.when(adminAuthService.login(any(AdminLoginRequest.class)))
                .thenThrow(new IllegalArgumentException("用户名或密码错误"));

        mockMvc.perform(post("/admin/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    /**
     * 测试：查询当前用户信息成功。
     */
    @Test
    void me_shouldReturnCurrentUser() throws Exception {
        AdminAuthContext.setCurrentUser(new AdminUserInfo(1L, "admin", "管理员"));

        mockMvc.perform(get("/admin/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nickname").value("管理员"));
    }

    /**
     * 测试：退出登录成功。
     */
    @Test
    void logout_shouldReturnOk() throws Exception {
        Mockito.when(adminAuthService.extractToken("Bearer token123")).thenReturn("token123");

        mockMvc.perform(post("/admin/auth/logout")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk());

        Mockito.verify(adminAuthService).logout("token123");
    }
}
