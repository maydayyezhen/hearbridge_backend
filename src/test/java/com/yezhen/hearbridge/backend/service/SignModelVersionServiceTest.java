package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 模型版本 Service 白盒测试。
 */
@ExtendWith(MockitoExtension.class)
class SignModelVersionServiceTest {

    /**
     * 模型版本 Mapper Mock。
     */
    @Mock
    private SignModelVersionMapper signModelVersionMapper;

    /**
     * 被测试的模型版本 Service。
     */
    private SignModelVersionService signModelVersionService;

    /**
     * 每个测试用例执行前初始化 Service。
     */
    @BeforeEach
    void setUp() {
        signModelVersionService = new SignModelVersionService(
                signModelVersionMapper,
                new ObjectMapper()
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

        doAnswer(invocation -> {
            SignModelVersion version = invocation.getArgument(0);
            version.setId(1L);
            return 1;
        }).when(signModelVersionMapper).insert(any(SignModelVersion.class));

        when(signModelVersionMapper.selectById(1L)).thenReturn(saved);

        SignModelVersion result = signModelVersionService.createFromTrainResult(trainResult);

        assertEquals(1L, result.getId());
        assertEquals("train_20260426_153000", result.getRunName());
        assertEquals("TRAINED", result.getStatus());
        assertFalse(result.getPublished());

        verify(signModelVersionMapper).selectByRunName("train_20260426_153000");
        verify(signModelVersionMapper).insert(any(SignModelVersion.class));
        verify(signModelVersionMapper).selectById(1L);
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

        verify(signModelVersionMapper).selectByRunName("train_20260426_153000");
        verify(signModelVersionMapper, never()).insert(any());
    }

    /**
     * 测试：发布模型版本成功。
     */
    @Test
    void publish_shouldClearOldPublishedAndPublishTargetVersion() {
        SignModelVersion oldVersion = buildVersion(1L, false, "TRAINED");
        SignModelVersion publishedVersion = buildVersion(1L, true, "PUBLISHED");

        when(signModelVersionMapper.selectById(1L)).thenReturn(oldVersion, publishedVersion);
        when(signModelVersionMapper.clearPublished()).thenReturn(1);
        when(signModelVersionMapper.publishById(1L)).thenReturn(1);

        SignModelVersion result = signModelVersionService.publish(1L);

        assertEquals(1L, result.getId());
        assertEquals("PUBLISHED", result.getStatus());
        assertTrue(result.getPublished());

        verify(signModelVersionMapper, times(2)).selectById(1L);
        verify(signModelVersionMapper).clearPublished();
        verify(signModelVersionMapper).publishById(1L);
    }

    /**
     * 测试：发布不存在的模型版本时，应抛出异常。
     */
    @Test
    void publish_shouldThrowException_whenVersionNotExists() {
        when(signModelVersionMapper.selectById(404L)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signModelVersionService.publish(404L)
        );

        assertEquals("模型版本不存在，ID：404", exception.getMessage());

        verify(signModelVersionMapper).selectById(404L);
        verify(signModelVersionMapper, never()).clearPublished();
        verify(signModelVersionMapper, never()).publishById(anyLong());
    }

    /**
     * 构造训练结果。
     *
     * @return 训练结果
     */
    private ModelTrainResult buildTrainResult() {
        ModelTrainResult result = new ModelTrainResult();

        result.setRunName("train_20260426_153000");
        result.setDataRoot("data_processed_arm_pose_10fps");
        result.setArtifactDir("artifacts/train_20260426_153000");
        result.setModelPath("artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        result.setLabelMapPath("artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        result.setTrainingCurvePath("artifacts/train_20260426_153000/training_curve.png");
        result.setConfusionMatrixPath("artifacts/train_20260426_153000/confusion_matrix.png");
        result.setEvalResultPath("artifacts/train_20260426_153000/eval_result.txt");

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

    /**
     * 构造模型版本。
     *
     * @param id        ID
     * @param published 是否发布
     * @param status    状态
     * @return 模型版本
     */
    private SignModelVersion buildVersion(Long id, Boolean published, String status) {
        SignModelVersion version = new SignModelVersion();

        version.setId(id);
        version.setVersionName("train_20260426_153000");
        version.setRunName("train_20260426_153000");
        version.setModelPath("artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        version.setLabelMapPath("artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        version.setSampleCount(80);
        version.setClassCount(10);
        version.setFinalValAccuracy(new BigDecimal("0.8750"));
        version.setPublished(published);
        version.setStatus(status);

        return version;
    }
}
