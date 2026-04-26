package com.yezhen.hearbridge.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson JSON 配置。
 *
 * 当前用途：
 * 1. 为业务 Service 提供 ObjectMapper Bean；
 * 2. 用于模型版本管理中 labelMap 的 JSON 序列化。
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建 ObjectMapper Bean。
     *
     * @return ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 避免将日期时间序列化为时间戳。
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
