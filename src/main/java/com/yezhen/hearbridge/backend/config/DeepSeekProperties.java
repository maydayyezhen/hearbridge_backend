package com.yezhen.hearbridge.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hearbridge.deepseek")
public class DeepSeekProperties {

    /**
     * DeepSeek API 基础地址。
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * API Key。不要硬编码真实 key。
     */
    private String apiKey;

    /**
     * 模型名称。
     */
    private String model = "deepseek-v4-flash";

    /**
     * 请求超时时间，单位秒。
     */
    private Integer timeoutSeconds = 30;
}
