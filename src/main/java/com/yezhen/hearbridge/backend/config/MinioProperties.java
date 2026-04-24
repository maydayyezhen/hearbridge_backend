package com.yezhen.hearbridge.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MinIO access configuration.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hearbridge.minio")
public class MinioProperties {

    private String endpoint;

    private String publicEndpoint;

    private String bucket;

    private String accessKey;

    private String secretKey;

    public String buildObjectUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }

        String normalizedEndpoint = StringUtils.hasText(publicEndpoint) ? publicEndpoint : endpoint;
        if (normalizedEndpoint.endsWith("/")) {
            normalizedEndpoint = normalizedEndpoint.substring(0, normalizedEndpoint.length() - 1);
        }

        String normalizedObjectKey = objectKey;
        if (normalizedObjectKey.startsWith("/")) {
            normalizedObjectKey = normalizedObjectKey.substring(1);
        }

        return normalizedEndpoint + "/" + bucket + "/" + normalizedObjectKey;
    }
}
