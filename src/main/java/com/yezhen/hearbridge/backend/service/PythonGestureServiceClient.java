package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.PythonServiceProperties;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;

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

    /**
     * 调用 Python 服务执行 raw → feature 转换。
     *
     * @return 转换结果
     */
    public FeatureConvertResult convertRawToFeatures() {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return restClient.post()
                .uri(normalizedBaseUrl + "/dataset/raw/convert-to-features")
                .retrieve()
                .body(FeatureConvertResult.class);
    }

    /**
     * 调用 Python 服务执行模型训练。
     *
     * @return 模型训练结果
     */
    public ModelTrainResult trainModel() {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return restClient.post()
                .uri(normalizedBaseUrl + "/model/train")
                .retrieve()
                .body(ModelTrainResult.class);
    }

}
