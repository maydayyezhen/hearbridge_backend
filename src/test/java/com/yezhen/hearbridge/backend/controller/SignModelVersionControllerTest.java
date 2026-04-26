package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.service.SignModelVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 模型版本 Controller 黑盒测试。
 */
class SignModelVersionControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * 模型版本 Service Mock。
     */
    private SignModelVersionService signModelVersionService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        signModelVersionService = Mockito.mock(SignModelVersionService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignModelVersionController(signModelVersionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：查询模型版本列表。
     */
    @Test
    void listModelVersions_shouldReturnVersionList() throws Exception {
        SignModelVersion version = buildVersion(1L, false, "TRAINED");

        Mockito.when(signModelVersionService.listAll()).thenReturn(List.of(version));

        mockMvc.perform(get("/sign/model-versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].runName").value("train_20260426_153000"))
                .andExpect(jsonPath("$[0].sampleCount").value(80))
                .andExpect(jsonPath("$[0].classCount").value(10))
                .andExpect(jsonPath("$[0].status").value("TRAINED"))
                .andExpect(jsonPath("$[0].published").value(false));
    }

    /**
     * 测试：查询当前发布版本。
     */
    @Test
    void getPublishedVersion_shouldReturnPublishedVersion() throws Exception {
        SignModelVersion version = buildVersion(1L, true, "PUBLISHED");

        Mockito.when(signModelVersionService.getPublished()).thenReturn(version);

        mockMvc.perform(get("/sign/model-versions/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.published").value(true));
    }

    /**
     * 测试：发布模型版本。
     */
    @Test
    void publishModelVersion_shouldReturnPublishedVersion() throws Exception {
        SignModelVersion version = buildVersion(1L, true, "PUBLISHED");

        Mockito.when(signModelVersionService.publish(1L)).thenReturn(version);

        mockMvc.perform(put("/sign/model-versions/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
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
        version.setTrainSampleCount(64);
        version.setValSampleCount(16);
        version.setClassCount(10);
        version.setInputShape("30x166");
        version.setFinalValAccuracy(new BigDecimal("0.8750"));
        version.setStatus(status);
        version.setPublished(published);

        return version;
    }
}
