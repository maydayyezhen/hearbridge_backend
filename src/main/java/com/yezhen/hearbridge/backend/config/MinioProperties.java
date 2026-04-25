package com.yezhen.hearbridge.backend.config;

import com.yezhen.hearbridge.backend.util.LocalNetworkAddressUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * MinIO 访问配置。
 *
 * 职责说明：
 * 1. endpoint：后端服务自己访问 MinIO 使用的内部地址；
 * 2. publicEndpoint：浏览器 / 手机端访问 MinIO 静态资源时使用的公开地址；
 * 3. 如果 publicEndpoint 未配置，则根据 endpoint 自动推导公开访问地址。
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hearbridge.minio")
public class MinioProperties {

    /**
     * 后端访问 MinIO 的内部地址。
     *
     * 本地开发通常为：
     * http://localhost:9000
     *
     * Docker 部署时可能为：
     * http://minio:9000
     */
    private String endpoint;

    /**
     * 浏览器 / 手机端访问 MinIO 对象时使用的公开地址。
     *
     * 本地开发阶段一般不配置，交给代码自动推导当前 WLAN IP。
     * 部署到服务器后，可以显式配置为公网域名或服务器地址。
     */
    private String publicEndpoint;

    /**
     * MinIO 桶名称。
     */
    private String bucket;

    /**
     * MinIO AccessKey。
     */
    private String accessKey;

    /**
     * MinIO SecretKey。
     */
    private String secretKey;

    /**
     * 缓存后的公开访问地址。
     *
     * 避免每次拼接对象 URL 时都重复扫描网卡。
     * 如果中途切换 Wi-Fi，需要重启后端重新推导。
     */
    private String resolvedPublicEndpoint;

    /**
     * 获取后端访问 MinIO 使用的内部地址。
     *
     * @return 后端内部访问 MinIO 的 endpoint
     */
    public String getInternalEndpoint() {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("MinIO endpoint 未配置，后端无法访问 MinIO");
        }
        return endpoint;
    }

    /**
     * 根据对象 Key 构建浏览器 / 手机端可访问的完整对象 URL。
     *
     * @param objectKey MinIO 对象 Key
     * @return 完整访问 URL
     */
    public String buildObjectUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }

        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("MinIO bucket 未配置，无法构建对象访问地址");
        }

        String normalizedEndpoint = resolvePublicEndpoint();
        if (!StringUtils.hasText(normalizedEndpoint)) {
            throw new IllegalStateException("MinIO endpoint/publicEndpoint 未配置，无法构建对象访问地址");
        }

        if (normalizedEndpoint.endsWith("/")) {
            normalizedEndpoint = normalizedEndpoint.substring(0, normalizedEndpoint.length() - 1);
        }

        String normalizedObjectKey = objectKey;
        if (normalizedObjectKey.startsWith("/")) {
            normalizedObjectKey = normalizedObjectKey.substring(1);
        }

        return normalizedEndpoint + "/" + bucket + "/" + normalizedObjectKey;
    }

    /**
     * 解析浏览器 / 手机端访问 MinIO 对象时使用的公开地址。
     *
     * 优先级：
     * 1. 如果显式配置 publicEndpoint，直接使用；
     * 2. 如果没有配置 publicEndpoint，则根据 endpoint 自动推导；
     * 3. endpoint 是 localhost / 127.0.0.1 / 0.0.0.0 / minio 时，替换为当前 WLAN IP。
     *
     * @return 公开访问地址
     */
    private String resolvePublicEndpoint() {
        if (StringUtils.hasText(resolvedPublicEndpoint)) {
            return resolvedPublicEndpoint;
        }

        if (StringUtils.hasText(publicEndpoint)) {
            resolvedPublicEndpoint = publicEndpoint;
            log.info("MinIO public endpoint 使用显式配置：{}", resolvedPublicEndpoint);
            return resolvedPublicEndpoint;
        }

        resolvedPublicEndpoint = autoBuildPublicEndpointFromEndpoint(endpoint);

        log.info(
                "MinIO public endpoint 自动推导结果：endpoint={}, resolvedPublicEndpoint={}",
                endpoint,
                resolvedPublicEndpoint
        );

        return resolvedPublicEndpoint;
    }

    /**
     * 根据后端内部 endpoint 自动推导浏览器 / 手机端可访问的 endpoint。
     *
     * @param sourceEndpoint 原始 endpoint
     * @return 推导后的公开 endpoint
     */
    private String autoBuildPublicEndpointFromEndpoint(String sourceEndpoint) {
        if (!StringUtils.hasText(sourceEndpoint)) {
            return null;
        }

        try {
            URI uri = URI.create(sourceEndpoint);
            String scheme = StringUtils.hasText(uri.getScheme()) ? uri.getScheme() : "http";
            String host = uri.getHost();
            int port = uri.getPort();

            if (!StringUtils.hasText(host)) {
                return sourceEndpoint;
            }

            String publicHost = shouldReplaceHost(host)
                    ? LocalNetworkAddressUtil.resolveLocalLanIp()
                    : host;

            StringBuilder builder = new StringBuilder();
            builder.append(scheme).append("://").append(publicHost);

            if (port > 0) {
                builder.append(":").append(port);
            }

            return builder.toString();
        } catch (Exception ex) {
            log.warn("MinIO public endpoint 自动推导失败，sourceEndpoint={}", sourceEndpoint, ex);
            return sourceEndpoint;
        }
    }

    /**
     * 判断 endpoint 中的 host 是否需要替换成本机局域网 IP。
     *
     * @param host 原始 host
     * @return 是否需要替换
     */
    private boolean shouldReplaceHost(String host) {
        String normalizedHost = host.toLowerCase();

        return "localhost".equals(normalizedHost)
                || "127.0.0.1".equals(normalizedHost)
                || "0.0.0.0".equals(normalizedHost)
                || "minio".equals(normalizedHost);
    }
}
