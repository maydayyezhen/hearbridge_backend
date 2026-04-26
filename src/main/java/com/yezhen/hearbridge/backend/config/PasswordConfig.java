package com.yezhen.hearbridge.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码哈希配置。
 *
 * 当前只使用 BCrypt 密码哈希能力，
 * 不启用 Spring Security 登录认证体系。
 */
@Configuration
public class PasswordConfig {

    /**
     * 创建密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
