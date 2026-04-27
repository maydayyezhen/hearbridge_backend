package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.ModelTrainResult;
import com.yezhen.hearbridge.backend.service.SignSampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 模型训练接口黑盒测试。
 */
class SignSampleTrainControllerTest {

    private MockMvc mockMvc;

    private SignSampleService signSampleService;

    @BeforeEach
    void setUp() {
        signSampleService = Mockito.mock(SignSampleService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignSampleController(signSampleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：POST /sign/samples/train 返回 MinIO objectKey 形式的训练产物路径。
     */
    @Test
    void trainModel_shouldReturnTrainResultWithMinioArtifactObjectKeys() throws Exception {
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
        result.setVersionId(1L);
        result.setVersionName("train_20260426_153000");
        result.setVersionStatus("TRAINED");
        result.setPublished(false);
        result.setMessage("training completed");

        Mockito.when(signSampleService.trainModel()).thenReturn(result);

        mockMvc.perform(post("/sign/samples/train"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runName").value("train_20260426_153000"))
                .andExpect(jsonPath("$.modelPath").value("sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras"))
                .andExpect(jsonPath("$.trainingCurvePath").value("sign/model/artifacts/train_20260426_153000/training_curve.png"))
                .andExpect(jsonPath("$.confusionMatrixPath").value("sign/model/artifacts/train_20260426_153000/confusion_matrix.png"))
                .andExpect(jsonPath("$.versionId").value(1))
                .andExpect(jsonPath("$.versionStatus").value("TRAINED"))
                .andExpect(jsonPath("$.published").value(false));
    }

    /**
     * 测试：POST /sign/samples/train 训练失败时返回 400。
     */
    @Test
    void trainModel_shouldReturnBadRequest_whenServiceThrowsException() throws Exception {
        Mockito.when(signSampleService.trainModel())
                .thenThrow(new IllegalArgumentException("训练产物为空：train_xxx/model.keras"));

        mockMvc.perform(post("/sign/samples/train"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("训练产物为空：train_xxx/model.keras"));
    }
}
