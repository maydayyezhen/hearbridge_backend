package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.PythonServiceProperties;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Python 手势识别服务客户端。
 *
 * 当前职责：
 * 1. 调用 Python raw dataset 扫描接口；
 * 2. 获取样本摘要列表。
 */
@Service
public class PythonGestureServiceClient {

    /**
     * Python 服务配置。
     */
    private final PythonServiceProperties pythonServiceProperties;

    /**
     * RestClient。
     */
    private final RestClient restClient;

    /**
     * 构造注入。
     *
     * @param pythonServiceProperties Python 服务配置
     */
    public PythonGestureServiceClient(PythonServiceProperties pythonServiceProperties) {
        this.pythonServiceProperties = pythonServiceProperties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * 获取 Python 服务扫描到的 raw 样本列表。
     *
     * @return raw 样本列表响应
     */
    public PythonRawSampleListResponse listRawSamples() {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return restClient.get()
                .uri(normalizedBaseUrl + "/dataset/raw/samples")
                .retrieve()
                .body(PythonRawSampleListResponse.class);
    }
}
