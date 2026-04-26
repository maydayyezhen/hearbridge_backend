package com.yezhen.hearbridge.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python 手势识别服务配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hearbridge.python")
public class PythonServiceProperties {

    /**
     * Python 手势识别服务基础地址。
     *
     * 本地开发通常为：
     * http://127.0.0.1:8000
     */
    private String gestureServiceBaseUrl;
}
