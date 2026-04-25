package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class MinioStorageService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final MinioProperties minioProperties;

    public MinioStorageService(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    public String uploadAvatar(Long userId,
                               InputStream inputStream,
                               long size,
                               String originalFileName,
                               String contentType) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        }
        if (size == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        String normalizedContentType = normalizeImageContentType(contentType, originalFileName);
        String extension = extensionByContentType(normalizedContentType);
        String objectKey = "images/avatar/" + userId + "/"
                + LocalDate.now().format(DATE_FORMAT) + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;

        try {
            MinioClient client = buildClient();
            ensureBucket(client);
            PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .contentType(normalizedContentType);
            if (size > 0) {
                putObjectBuilder.stream(inputStream, size, -1);
            } else {
                putObjectBuilder.stream(inputStream, -1, 10 * 1024 * 1024);
            }
            client.putObject(
                    putObjectBuilder.build()
            );
            return objectKey;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar upload failed", ex);
        }
    }

    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getInternalEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    private void ensureBucket(MinioClient client) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .build()
        );
        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );
        }
    }

    private String normalizeImageContentType(String contentType, String originalFileName) {
        String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (lowerContentType.startsWith("image/")) {
            if (lowerContentType.contains("png")) {
                return "image/png";
            }
            if (lowerContentType.contains("webp")) {
                return "image/webp";
            }
            return "image/jpeg";
        }

        String lowerName = StringUtils.hasText(originalFileName) ? originalFileName.toLowerCase(Locale.ROOT) : "";
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files can be uploaded as avatars");
    }

    private String extensionByContentType(String contentType) {
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
