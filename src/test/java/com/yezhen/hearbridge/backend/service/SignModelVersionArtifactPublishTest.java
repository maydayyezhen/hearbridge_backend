package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.dto.ModelReloadResult;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.mapper.SignModelVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模型版本发布白盒测试。
 */
@ExtendWith(MockitoExtension.class)
class SignModelVersionArtifactPublishTest {

    @Mock
    private SignModelVersionMapper signModelVersionMapper;

    @Mock
    private PythonGestureServiceClient pythonGestureServiceClient;

    @Mock
    private MinioProperties minioProperties;

    private SignModelVersionService signModelVersionService;

    @BeforeEach
    void setUp() {
        signModelVersionService = new SignModelVersionService(
                signModelVersionMapper,
                new ObjectMapper(),
                pythonGestureServiceClient,
                minioProperties
        );
    }

    /**
     * 测试：MinIO objectKey 模型版本发布时，应调用 reloadModelFromUrl。
     */
    @Test
    void publish_shouldReloadModelFromUrl_whenVersionUsesMinioObjectKeys() {
        SignModelVersion oldVersion = buildVersion(false, "TRAINED", true);
        SignModelVersion publishedVersion = buildVersion(true, "PUBLISHED", true);

        when(signModelVersionMapper.selectById(1L)).thenReturn(oldVersion, publishedVersion);
        when(minioProperties.buildObjectUrl(anyString()))
                .thenAnswer(invocation -> "http://localhost:9000/hearbridge/" + invocation.getArgument(0, String.class));
        when(pythonGestureServiceClient.reloadModelFromUrl(
                "http://localhost:9000/hearbridge/" + oldVersion.getModelPath(),
                "http://localhost:9000/hearbridge/" + oldVersion.getLabelMapPath(),
                oldVersion.getVersionName()
        )).thenReturn(successReloadResult(oldVersion));

        SignModelVersion result = signModelVersionService.publish(1L);

        assertEquals("PUBLISHED", result.getStatus());
        assertTrue(result.getPublished());
        verify(pythonGestureServiceClient).reloadModelFromUrl(
                "http://localhost:9000/hearbridge/" + oldVersion.getModelPath(),
                "http://localhost:9000/hearbridge/" + oldVersion.getLabelMapPath(),
                oldVersion.getVersionName()
        );
        verify(pythonGestureServiceClient, never()).reloadModel(anyString(), anyString(), anyString());
        verify(signModelVersionMapper).clearPublished();
        verify(signModelVersionMapper).publishById(1L);
    }

    /**
     * 测试：历史本地路径模型版本发布时，应继续调用旧 reloadModel。
     */
    @Test
    void publish_shouldUseLegacyReload_whenVersionUsesLocalPaths() {
        SignModelVersion oldVersion = buildVersion(false, "TRAINED", false);
        SignModelVersion publishedVersion = buildVersion(true, "PUBLISHED", false);

        when(signModelVersionMapper.selectById(1L)).thenReturn(oldVersion, publishedVersion);
        when(pythonGestureServiceClient.reloadModel(
                oldVersion.getModelPath(),
                oldVersion.getLabelMapPath(),
                oldVersion.getVersionName()
        )).thenReturn(successReloadResult(oldVersion));

        SignModelVersion result = signModelVersionService.publish(1L);

        assertEquals("PUBLISHED", result.getStatus());
        assertTrue(result.getPublished());
        verify(pythonGestureServiceClient).reloadModel(
                oldVersion.getModelPath(),
                oldVersion.getLabelMapPath(),
                oldVersion.getVersionName()
        );
        verify(pythonGestureServiceClient, never()).reloadModelFromUrl(anyString(), anyString(), anyString());
    }

    /**
     * 测试：Python reload 失败时，不应更新数据库发布状态。
     */
    @Test
    void publish_shouldThrowExceptionAndNotPublish_whenPythonReloadFailed() {
        SignModelVersion version = buildVersion(false, "TRAINED", true);
        ModelReloadResult reloadResult = new ModelReloadResult();
        reloadResult.setOk(false);
        reloadResult.setMessage("reload failed");

        when(signModelVersionMapper.selectById(1L)).thenReturn(version);
        when(minioProperties.buildObjectUrl(anyString()))
                .thenAnswer(invocation -> "http://localhost:9000/hearbridge/" + invocation.getArgument(0, String.class));
        when(pythonGestureServiceClient.reloadModelFromUrl(anyString(), anyString(), anyString()))
                .thenReturn(reloadResult);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signModelVersionService.publish(1L)
        );

        assertEquals("Python 服务重载模型失败", exception.getMessage());
        verify(signModelVersionMapper, never()).clearPublished();
        verify(signModelVersionMapper, never()).publishById(anyLong());
    }

    private SignModelVersion buildVersion(Boolean published, String status, boolean minioPath) {
        SignModelVersion version = new SignModelVersion();
        String prefix = minioPath
                ? "sign/model/artifacts/train_20260426_153000/"
                : "D:/MySssb/artifacts/train_20260426_153000/";

        version.setId(1L);
        version.setVersionName("train_20260426_153000");
        version.setRunName("train_20260426_153000");
        version.setModelPath(prefix + "gesture_cnn_arm_pose_10fps.keras");
        version.setLabelMapPath(prefix + "label_map_arm_pose_10fps.json");
        version.setPublished(published);
        version.setStatus(status);
        return version;
    }

    private ModelReloadResult successReloadResult(SignModelVersion version) {
        ModelReloadResult reloadResult = new ModelReloadResult();
        reloadResult.setOk(true);
        reloadResult.setVersionName(version.getVersionName());
        reloadResult.setModelPath(version.getModelPath());
        reloadResult.setLabelMapPath(version.getLabelMapPath());
        reloadResult.setMessage("model reloaded");
        return reloadResult;
    }
}
