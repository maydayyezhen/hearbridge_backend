package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.dto.FileUploadResult;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * MinIO 文件存储服务。
 *
 * 职责：
 * 1. 构建 MinIO Client；
 * 2. 确保 bucket 存在；
 * 3. 按业务类型生成 objectKey；
 * 4. 上传文件到 MinIO；
 * 5. 返回 objectKey + 可访问 URL。
 */
@Service
public class MinioStorageService {

    /**
     * 日期格式，用于生成对象路径。
     */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * 图片最大大小：5MB。
     */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    /**
     * SiGML 文件最大大小：2MB。
     */
    private static final long MAX_SIGML_SIZE = 2 * 1024 * 1024;

    /**
     * MinIO 配置。
     */
    private final MinioProperties minioProperties;

    /**
     * 构造注入 MinIO 配置。
     *
     * @param minioProperties MinIO 配置
     */
    public MinioStorageService(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    /**
     * 按业务类型上传文件。
     *
     * 当前支持：
     * 1. sign-category-cover：手势分类封面图片；
     * 2. sign-resource-cover：手势资源封面图片；
     * 3. sign-resource-sigml：手势资源 SiGML 文件。
     *
     * @param file    上传文件
     * @param bizType 业务类型
     * @return 上传结果
     */
    public FileUploadResult uploadByBizType(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }

        UploadTarget uploadTarget = resolveUploadTarget(bizType, file);

        String objectKey = buildObjectKey(uploadTarget.directory(), uploadTarget.extension());

        try (InputStream inputStream = file.getInputStream()) {
            MinioClient client = buildClient();
            ensureBucket(client);

            System.out.println("MinIO upload start, endpoint=" + minioProperties.getInternalEndpoint()
                    + ", bucket=" + minioProperties.getBucket()
                    + ", objectKey=" + objectKey
                    + ", size=" + file.getSize()
                    + ", contentType=" + uploadTarget.contentType());

            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .contentType(uploadTarget.contentType())
                            .stream(inputStream, file.getSize(), -1)
                            .build()
            );

            return new FileUploadResult(
                    minioProperties.getBucket(),
                    objectKey,
                    minioProperties.buildObjectUrl(objectKey),
                    file.getOriginalFilename(),
                    uploadTarget.contentType(),
                    file.getSize()
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败", ex);
        }
    }

    /**
     * 上传用户头像。
     *
     * 说明：
     * 保留原有头像上传能力，避免影响已有 App 用户头像功能。
     *
     * @param userId           用户 ID
     * @param inputStream      文件输入流
     * @param size             文件大小
     * @param originalFileName 原始文件名
     * @param contentType      文件类型
     * @return MinIO 对象 Key
     */
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
        String extension = extensionByImageContentType(normalizedContentType);
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
            client.putObject(putObjectBuilder.build());
            return objectKey;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar upload failed", ex);
        }
    }

    /**
     * 根据业务类型解析上传目录、文件类型和扩展名。
     *
     * @param bizType 业务类型
     * @param file    上传文件
     * @return 上传目标描述
     */
    private UploadTarget resolveUploadTarget(String bizType, MultipartFile file) {
        if (!StringUtils.hasText(bizType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bizType 不能为空");
        }

        String normalizedBizType = bizType.trim();

        return switch (normalizedBizType) {
            case "sign-category-cover" -> resolveImageTarget("sign/category/", file);
            case "sign-resource-cover" -> resolveImageTarget("sign/resource/cover/", file);
            case "sign-resource-sigml" -> resolveSigmlTarget("sign/resource/sigml/", file);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "不支持的上传业务类型：" + bizType
            );
        };
    }

    /**
     * 解析图片上传目标。
     *
     * @param directory MinIO 对象目录
     * @param file      上传文件
     * @return 上传目标描述
     */
    private UploadTarget resolveImageTarget(String directory, MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片大小不能超过 5MB");
        }

        String contentType = normalizeImageContentType(file.getContentType(), file.getOriginalFilename());
        String extension = extensionByImageContentType(contentType);

        return new UploadTarget(directory, contentType, extension);
    }

    /**
     * 解析 SiGML 上传目标。
     *
     * @param directory MinIO 对象目录
     * @param file      上传文件
     * @return 上传目标描述
     */
    private UploadTarget resolveSigmlTarget(String directory, MultipartFile file) {
        if (file.getSize() > MAX_SIGML_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SiGML 文件大小不能超过 2MB");
        }

        String originalFileName = file.getOriginalFilename();
        String lowerName = StringUtils.hasText(originalFileName)
                ? originalFileName.toLowerCase(Locale.ROOT)
                : "";

        if (!lowerName.endsWith(".sigml") && !lowerName.endsWith(".xml")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只允许上传 .sigml 或 .xml 文件");
        }

        String extension = lowerName.endsWith(".xml") ? ".xml" : ".sigml";

        return new UploadTarget(directory, "application/xml", extension);
    }

    /**
     * 构建 MinIO 对象 Key。
     *
     * @param directory  业务目录
     * @param extension 文件扩展名
     * @return MinIO 对象 Key
     */
    private String buildObjectKey(String directory, String extension) {
        return directory
                + LocalDate.now().format(DATE_FORMAT)
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;
    }

    /**
     * 构建 MinIO Client。
     *
     * @return MinIO Client
     */
    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getInternalEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * 确保 bucket 存在。
     *
     * @param client MinIO Client
     * @throws Exception MinIO 操作异常
     */
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

    /**
     * 归一化图片 Content-Type。
     *
     * 只允许：
     * 1. jpg / jpeg
     * 2. png
     * 3. webp
     *
     * @param contentType      请求中的 Content-Type
     * @param originalFileName 原始文件名
     * @return 归一化后的 Content-Type
     */
    private String normalizeImageContentType(String contentType, String originalFileName) {
        String lowerContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (lowerContentType.startsWith("image/")) {
            if (lowerContentType.contains("png")) {
                return "image/png";
            }
            if (lowerContentType.contains("webp")) {
                return "image/webp";
            }
            if (lowerContentType.contains("jpeg") || lowerContentType.contains("jpg")) {
                return "image/jpeg";
            }
        }

        String lowerName = StringUtils.hasText(originalFileName)
                ? originalFileName.toLowerCase(Locale.ROOT)
                : "";

        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只允许上传 jpg、png、webp 图片");
    }

    /**
     * 根据图片 Content-Type 获取扩展名。
     *
     * @param contentType 图片 Content-Type
     * @return 文件扩展名
     */
    private String extensionByImageContentType(String contentType) {
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    /**
     * 上传目标描述。
     *
     * @param directory   MinIO 对象目录
     * @param contentType 文件类型
     * @param extension   文件扩展名
     */
    private record UploadTarget(String directory, String contentType, String extension) {
    }
}
