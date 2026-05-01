package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.PythonServiceProperties;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import com.yezhen.hearbridge.backend.dto.SentenceVideoRecognizeResult;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.dto.ModelReloadRequest;
import com.yezhen.hearbridge.backend.dto.ModelReloadResult;
import com.yezhen.hearbridge.backend.dto.ModelReloadFromUrlRequest;

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
     * 获取规范化后的 Python 服务基础地址。
     *
     * @return 不以 / 结尾的 Python 服务基础地址
     */
    private String getNormalizedBaseUrl() {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
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

    /**
     * 调用 Python 服务重载实时识别模型。
     *
     * @param modelPath    模型文件路径
     * @param labelMapPath 标签映射文件路径
     * @param versionName  模型版本名称
     * @return 重载结果
     */
    public ModelReloadResult reloadModel(
            String modelPath,
            String labelMapPath,
            String versionName) {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        ModelReloadRequest request = new ModelReloadRequest(
                modelPath,
                labelMapPath,
                versionName
        );

        return restClient.post()
                .uri(normalizedBaseUrl + "/model/reload")
                .body(request)
                .retrieve()
                .body(ModelReloadResult.class);
    }

    /**
     * 下载 Python 训练产物文件。
     *
     * @param runName  训练运行名称
     * @param fileName 文件名
     * @return 文件字节
     */
    public byte[] downloadArtifact(String runName, String fileName) {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return restClient.get()
                .uri(normalizedBaseUrl + "/artifacts/{runName}/{fileName}", runName, fileName)
                .retrieve()
                .body(byte[].class);
    }

    /**
     * 通过 MinIO URL 重载 Python 当前模型。
     *
     * @param modelUrl    模型文件 URL
     * @param labelMapUrl 标签映射文件 URL
     * @param versionName 模型版本名称
     * @return 重载结果
     */
    public ModelReloadResult reloadModelFromUrl(
            String modelUrl,
            String labelMapUrl,
            String versionName) {
        String baseUrl = pythonServiceProperties.getGestureServiceBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Python 手势识别服务地址未配置");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        ModelReloadFromUrlRequest request = new ModelReloadFromUrlRequest(
                modelUrl,
                labelMapUrl,
                versionName
        );

        return restClient.post()
                .uri(normalizedBaseUrl + "/model/reload-from-url")
                .body(request)
                .retrieve()
                .body(ModelReloadResult.class);
    }

    /**
     * 调用 Python 服务进行句子视频识别。
     *
     * @param file 上传的视频文件
     * @return Python 句子视频识别结果
     */
    public SentenceVideoRecognizeResult recognizeSentenceVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传视频不能为空");
        }

        String normalizedBaseUrl = getNormalizedBaseUrl();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        /*
         * MultipartFile#getResource() 会保留文件名和长度信息，
         * 适合转发给 Python FastAPI 的 multipart/form-data 接口。
         */
        Resource fileResource = file.getResource();
        body.add("file", fileResource);

        return restClient.post()
                .uri(normalizedBaseUrl + "/api/sentence/recognize")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(SentenceVideoRecognizeResult.class);
    }

}
