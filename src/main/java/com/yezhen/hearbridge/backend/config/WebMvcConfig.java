package com.yezhen.hearbridge.backend.config;

import com.yezhen.hearbridge.backend.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 统一认证拦截器。
     */
    private final AuthInterceptor authInterceptor;

    /**
     * 构造注入。
     *
     * @param authInterceptor 统一认证拦截器
     */
    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 注册拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/app/**",
                        "/sign/**",
                        "/files/**"
                );
    }
}
