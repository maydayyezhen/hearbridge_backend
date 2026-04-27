package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.service.SignModelVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 模型版本接口黑盒测试。
 */
class SignModelVersionArtifactControllerTest {

    private MockMvc mockMvc;

    private SignModelVersionService signModelVersionService;

    @BeforeEach
    void setUp() {
        signModelVersionService = Mockito.mock(SignModelVersionService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignModelVersionController(signModelVersionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：查询模型版本列表时，应返回训练产物 URL 字段。
     */
    @Test
    void listModelVersions_shouldReturnArtifactUrls() throws Exception {
        SignModelVersion version = buildVersion(false, "TRAINED");
        Mockito.when(signModelVersionService.listAll()).thenReturn(List.of(version));

        mockMvc.perform(get("/sign/model-versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runName").value("train_20260426_153000"))
                .andExpect(jsonPath("$[0].modelUrl").value("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras"))
                .andExpect(jsonPath("$[0].trainingCurveUrl").value("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/training_curve.png"))
                .andExpect(jsonPath("$[0].confusionMatrixUrl").value("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/confusion_matrix.png"))
                .andExpect(jsonPath("$[0].status").value("TRAINED"))
                .andExpect(jsonPath("$[0].published").value(false));
    }

    /**
     * 测试：查询当前发布版本。
     */
    @Test
    void getPublishedVersion_shouldReturnPublishedVersion() throws Exception {
        SignModelVersion version = buildVersion(true, "PUBLISHED");
        Mockito.when(signModelVersionService.getPublished()).thenReturn(version);

        mockMvc.perform(get("/sign/model-versions/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.modelUrl").value("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras"));
    }

    /**
     * 测试：发布模型版本成功。
     */
    @Test
    void publishModelVersion_shouldReturnPublishedVersion() throws Exception {
        SignModelVersion version = buildVersion(true, "PUBLISHED");
        Mockito.when(signModelVersionService.publish(1L)).thenReturn(version);

        mockMvc.perform(put("/sign/model-versions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.published").value(true));
    }

    /**
     * 测试：发布不存在的模型版本时返回 400。
     */
    @Test
    void publishModelVersion_shouldReturnBadRequest_whenVersionNotExists() throws Exception {
        Mockito.when(signModelVersionService.publish(404L))
                .thenThrow(new IllegalArgumentException("模型版本不存在，ID：404"));

        mockMvc.perform(put("/sign/model-versions/404/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("模型版本不存在，ID：404"));
    }

    private SignModelVersion buildVersion(Boolean published, String status) {
        SignModelVersion version = new SignModelVersion();
        version.setId(1L);
        version.setVersionName("train_20260426_153000");
        version.setRunName("train_20260426_153000");
        version.setModelPath("sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        version.setLabelMapPath("sign/model/artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        version.setTrainingCurvePath("sign/model/artifacts/train_20260426_153000/training_curve.png");
        version.setConfusionMatrixPath("sign/model/artifacts/train_20260426_153000/confusion_matrix.png");
        version.setEvalResultPath("sign/model/artifacts/train_20260426_153000/eval_result.txt");
        version.setModelUrl("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/gesture_cnn_arm_pose_10fps.keras");
        version.setLabelMapUrl("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/label_map_arm_pose_10fps.json");
        version.setTrainingCurveUrl("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/training_curve.png");
        version.setConfusionMatrixUrl("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/confusion_matrix.png");
        version.setEvalResultUrl("http://localhost:9000/hearbridge/sign/model/artifacts/train_20260426_153000/eval_result.txt");
        version.setStatus(status);
        version.setPublished(published);
        return version;
    }
}
