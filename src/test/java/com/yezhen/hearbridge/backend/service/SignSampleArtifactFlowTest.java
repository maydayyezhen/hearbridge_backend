package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.FileUploadResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 训练产物接管流程白盒测试。
 */
@ExtendWith(MockitoExtension.class)
class SignSampleArtifactFlowTest {

    @Mock
    private SignSampleMapper signSampleMapper;

    @Mock
    private PythonGestureServiceClient pythonGestureServiceClient;

    @Mock
    private SignModelVersionService signModelVersionService;

    @Mock
    private MinioStorageService minioStorageService;

    private SignSampleService signSampleService;

    @BeforeEach
    void setUp() {
        signSampleService = new SignSampleService(
                signSampleMapper,
                pythonGestureServiceClient,
                signModelVersionService,
                minioStorageService
        );
    }

    /**
     * 测试：训练成功后，Spring Boot 应接管 Python 本地产物并上传 MinIO。
     */
    @Test
    void trainModel_shouldUploadArtifactsToMinioAndCreateModelVersion() {
        ModelTrainResult trainResult = buildTrainResult();
        SignModelVersion version = new SignModelVersion();
        version.setId(1L);
        version.setVersionName("train_20260426_153000");
        version.setStatus("TRAINED");
        version.setPublished(false);

        when(pythonGestureServiceClient.trainModel()).thenReturn(trainResult);
        when(pythonGestureServiceClient.downloadArtifact(eq("train_20260426_153000"), anyString()))
                .thenAnswer(invocation -> ("bytes-" + invocation.getArgument(1, String.class))
                        .getBytes(StandardCharsets.UTF_8));
        when(minioStorageService.uploadObject(any(InputStream.class), anyLong(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new FileUploadResult(
                        "hearbridge",
                        invocation.getArgument(2, String.class),
                        "http://localhost:9000/hearbridge/" + invocation.getArgument(2, String.class),
                        invocation.getArgument(4, String.class),
                        invocation.getArgument(3, String.class),
                        invocation.getArgument(1, Long.class)
                ));
        when(signModelVersionService.createFromTrainResult(any(ModelTrainResult.class))).thenReturn(version);

        ModelTrainResult actual = signSampleService.trainModel();

        assertEquals("sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras", actual.getModelPath());
        assertEquals("sign/model/artifacts/train_20260426_153000/label_map_arm_pose_10fps.json", actual.getLabelMapPath());
        assertEquals("sign/model/artifacts/train_20260426_153000/training_curve.png", actual.getTrainingCurvePath());
        assertEquals("sign/model/artifacts/train_20260426_153000/confusion_matrix.png", actual.getConfusionMatrixPath());
        assertEquals("sign/model/artifacts/train_20260426_153000/eval_result.txt", actual.getEvalResultPath());
        assertEquals(1L, actual.getVersionId());
        assertEquals("TRAINED", actual.getVersionStatus());
        assertFalse(actual.getPublished());

        verify(pythonGestureServiceClient).downloadArtifact("train_20260426_153000", "gesture_cnn_arm_pose_10fps.keras");
        verify(pythonGestureServiceClient).downloadArtifact("train_20260426_153000", "label_map_arm_pose_10fps.json");
        verify(pythonGestureServiceClient).downloadArtifact("train_20260426_153000", "training_curve.png");
        verify(pythonGestureServiceClient).downloadArtifact("train_20260426_153000", "confusion_matrix.png");
        verify(pythonGestureServiceClient).downloadArtifact("train_20260426_153000", "eval_result.txt");
        verify(minioStorageService, times(5)).uploadObject(any(InputStream.class), anyLong(), anyString(), anyString(), anyString());

        ArgumentCaptor<ModelTrainResult> captor = ArgumentCaptor.forClass(ModelTrainResult.class);
        verify(signModelVersionService).createFromTrainResult(captor.capture());
        assertTrue(captor.getValue().getModelPath().startsWith("sign/model/artifacts/"));
    }

    private ModelTrainResult buildTrainResult() {
        ModelTrainResult result = new ModelTrainResult();
        result.setRunName("train_20260426_153000");
        result.setModelPath("D:/MySssb/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        result.setLabelMapPath("D:/MySssb/artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        result.setTrainingCurvePath("D:/MySssb/artifacts/train_20260426_153000/training_curve.png");
        result.setConfusionMatrixPath("D:/MySssb/artifacts/train_20260426_153000/confusion_matrix.png");
        result.setEvalResultPath("D:/MySssb/artifacts/train_20260426_153000/eval_result.txt");
        result.setSampleCount(80);
        result.setTrainSampleCount(64);
        result.setValSampleCount(16);
        result.setClassCount(10);
        result.setInputShape(List.of(30, 166));
        result.setEpochsRan(20);
        result.setFinalTrainAccuracy(new BigDecimal("0.9500"));
        result.setFinalValAccuracy(new BigDecimal("0.8750"));
        result.setFinalTrainLoss(new BigDecimal("0.1200"));
        result.setFinalValLoss(new BigDecimal("0.2600"));
        result.setDurationSec(new BigDecimal("32.50"));
        result.setLabelMap(Map.of("hello", 0, "thanks", 1));
        result.setMessage("training completed");
        return result;
    }
}
