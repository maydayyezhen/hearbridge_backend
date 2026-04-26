package com.yezhen.hearbridge.backend.config;

import com.yezhen.hearbridge.backend.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 管理端认证拦截器。
     */
    private final AdminAuthInterceptor adminAuthInterceptor;

    /**
     * 构造注入。
     *
     * @param adminAuthInterceptor 管理端认证拦截器
     */
    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    /**
     * 注册拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/sign/**",
                        "/files/**"
                );
    }
}
