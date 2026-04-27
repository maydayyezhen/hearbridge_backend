package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.mapper.SignModelVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模型版本 Service 基础白盒测试。
 *
 * 模型发布分支由 SignModelVersionArtifactPublishTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class SignModelVersionServiceTest {

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
     * 测试：根据训练结果创建模型版本成功。
     */
    @Test
    void createFromTrainResult_shouldInsertVersion_whenRunNameNotExists() {
        ModelTrainResult trainResult = buildTrainResult();
        SignModelVersion saved = buildVersion(1L, false, "TRAINED");

        when(signModelVersionMapper.selectByRunName("train_20260426_153000")).thenReturn(null);
        when(signModelVersionMapper.insert(any(SignModelVersion.class))).thenAnswer(invocation -> {
            SignModelVersion version = invocation.getArgument(0);
            version.setId(1L);
            return 1;
        });
        when(signModelVersionMapper.selectById(1L)).thenReturn(saved);

        SignModelVersion result = signModelVersionService.createFromTrainResult(trainResult);

        assertEquals(1L, result.getId());
        assertEquals("train_20260426_153000", result.getRunName());
        assertEquals("TRAINED", result.getStatus());
        assertFalse(result.getPublished());
        verify(signModelVersionMapper).insert(any(SignModelVersion.class));
    }

    /**
     * 测试：如果训练运行名称已存在，应直接返回已有版本，避免重复入库。
     */
    @Test
    void createFromTrainResult_shouldReturnExistedVersion_whenRunNameExists() {
        ModelTrainResult trainResult = buildTrainResult();
        SignModelVersion existed = buildVersion(2L, false, "TRAINED");

        when(signModelVersionMapper.selectByRunName("train_20260426_153000")).thenReturn(existed);

        SignModelVersion result = signModelVersionService.createFromTrainResult(trainResult);

        assertEquals(2L, result.getId());
        verify(signModelVersionMapper, never()).insert(any(SignModelVersion.class));
    }

    private ModelTrainResult buildTrainResult() {
        ModelTrainResult result = new ModelTrainResult();
        result.setRunName("train_20260426_153000");
        result.setModelPath("sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        result.setLabelMapPath("sign/model/artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        result.setTrainingCurvePath("sign/model/artifacts/train_20260426_153000/training_curve.png");
        result.setConfusionMatrixPath("sign/model/artifacts/train_20260426_153000/confusion_matrix.png");
        result.setEvalResultPath("sign/model/artifacts/train_20260426_153000/eval_result.txt");
        result.setSampleCount(80);
        result.setTrainSampleCount(64);
        result.setValSampleCount(16);
        result.setClassCount(10);
        result.setInputShape(List.of(30, 166));
        result.setFinalTrainAccuracy(new BigDecimal("0.9500"));
        result.setFinalValAccuracy(new BigDecimal("0.8750"));
        result.setFinalTrainLoss(new BigDecimal("0.1200"));
        result.setFinalValLoss(new BigDecimal("0.2600"));
        result.setDurationSec(new BigDecimal("32.50"));
        result.setLabelMap(Map.of("hello", 0, "thanks", 1));
        return result;
    }

    private SignModelVersion buildVersion(Long id, Boolean published, String status) {
        SignModelVersion version = new SignModelVersion();
        version.setId(id);
        version.setVersionName("train_20260426_153000");
        version.setRunName("train_20260426_153000");
        version.setStatus(status);
        version.setPublished(published);
        return version;
    }
}
