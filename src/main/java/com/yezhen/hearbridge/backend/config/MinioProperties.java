package com.yezhen.hearbridge.backend.config;

import com.yezhen.hearbridge.backend.util.LocalNetworkAddressUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * MinIO 访问配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hearbridge.minio")
public class MinioProperties {

    /**
     * 服务端访问 MinIO 的地址。
     *
     * 例如：
     * http://localhost:9000
     * http://minio:9000
     */
    private String endpoint;

    /**
     * 浏览器访问 MinIO 对象时使用的公开地址。
     *
     * 如果配置了该字段，则优先使用它。
     * 如果没有配置，则会根据 endpoint 自动推导。
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
     * 缓存后的公开访问地址，避免每次拼 URL 都重复扫描网卡。
     */
    private String resolvedPublicEndpoint;

    /**
     * 根据对象 Key 构建浏览器可访问的完整对象 URL。
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

        String normalizedEndpoint = resolvePublicEndpoint();
        if (!StringUtils.hasText(normalizedEndpoint)) {
            return objectKey;
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
     * 解析浏览器访问 MinIO 对象时使用的公开地址。
     *
     * 优先级：
     * 1. 配置项 publicEndpoint；
     * 2. 根据 endpoint 自动推导；
     * 3. endpoint 原值。
     *
     * @return 公开访问地址
     */
    private String resolvePublicEndpoint() {
        if (StringUtils.hasText(resolvedPublicEndpoint)) {
            return resolvedPublicEndpoint;
        }

        if (StringUtils.hasText(publicEndpoint)) {
            resolvedPublicEndpoint = publicEndpoint;
            return resolvedPublicEndpoint;
        }

        resolvedPublicEndpoint = autoBuildPublicEndpointFromEndpoint(endpoint);
        return resolvedPublicEndpoint;
    }

    /**
     * 根据服务端 endpoint 自动推导浏览器可访问的 endpoint。
     *
     * 当 endpoint 使用 localhost、127.0.0.1、0.0.0.0、minio 等地址时，
     * 浏览器或手机端不一定能访问，因此这里替换成本机局域网 IPv4。
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
        } catch (Exception ignored) {
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
