package com.yezhen.hearbridge.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.service.SignSampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;

/**
 * 手势样本 Controller 黑盒测试。
 *
 * 使用 MockMvc 模拟 HTTP 请求，
 * 只验证接口输入输出，不依赖真实数据库。
 */
class SignSampleControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * JSON 序列化工具。
     */
    private ObjectMapper objectMapper;

    /**
     * 手势样本 Service Mock。
     */
    private SignSampleService signSampleService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        signSampleService = Mockito.mock(SignSampleService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignSampleController(signSampleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：GET /sign/samples 查询样本分页列表。
     */
    @Test
    void listSamples_shouldReturnPageResult() throws Exception {
        SignSample sample = buildSample(1L, "sample_a_001", "a", "GOOD", false);

        SignSamplePageResult pageResult = new SignSamplePageResult(
                List.of(sample),
                1L,
                1,
                20
        );

        Mockito.when(signSampleService.list(any())).thenReturn(pageResult);

        mockMvc.perform(get("/sign/samples")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].sampleCode").value("sample_a_001"))
                .andExpect(jsonPath("$.items[0].resourceCode").value("a"))
                .andExpect(jsonPath("$.items[0].qualityStatus").value("GOOD"));
    }

    /**
     * 测试：GET /sign/samples 支持按 resourceCode 和 qualityStatus 查询。
     */
    @Test
    void listSamples_shouldAcceptQueryParams() throws Exception {
        Mockito.when(signSampleService.list(any()))
                .thenReturn(new SignSamplePageResult(List.of(), 0L, 1, 10));

        mockMvc.perform(get("/sign/samples")
                        .param("resourceCode", "a")
                        .param("qualityStatus", "BAD")
                        .param("deleted", "false")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    /**
     * 测试：GET /sign/samples/summary 查询样本统计。
     */
    @Test
    void getSummary_shouldReturnSummary() throws Exception {
        SignSampleSummary summary = new SignSampleSummary(
                10L,
                3L,
                6L,
                2L,
                1L,
                1L
        );

        Mockito.when(signSampleService.summary()).thenReturn(summary);

        mockMvc.perform(get("/sign/samples/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.resourceCount").value(3))
                .andExpect(jsonPath("$.goodCount").value(6))
                .andExpect(jsonPath("$.warningCount").value(2))
                .andExpect(jsonPath("$.badCount").value(1))
                .andExpect(jsonPath("$.unknownCount").value(1));
    }

    /**
     * 测试：DELETE /sign/samples/{id} 软删除样本。
     */
    @Test
    void deleteSample_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/sign/samples/1"))
                .andExpect(status().isOk());

        Mockito.verify(signSampleService).deleteById(1L);
    }

    /**
     * 测试：DELETE /sign/samples/{id} 删除不存在样本时返回 400。
     */
    @Test
    void deleteSample_shouldReturnBadRequest_whenSampleNotExists() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("样本不存在，ID：404"))
                .when(signSampleService)
                .deleteById(404L);

        mockMvc.perform(delete("/sign/samples/404"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("样本不存在，ID：404"));
    }

    /**
     * 测试：PUT /sign/samples/{id}/quality 更新质量状态。
     */
    @Test
    void updateQuality_shouldReturnUpdatedSample() throws Exception {
        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("BAD");
        request.setQualityMessage("人工标记：手部关键点缺失");

        SignSample updatedSample = buildSample(1L, "sample_a_001", "a", "BAD", false);
        updatedSample.setQualityMessage("人工标记：手部关键点缺失");

        Mockito.when(signSampleService.updateQuality(eq(1L), any(SignSampleQualityUpdateRequest.class)))
                .thenReturn(updatedSample);

        mockMvc.perform(put("/sign/samples/1/quality")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sampleCode").value("sample_a_001"))
                .andExpect(jsonPath("$.qualityStatus").value("BAD"))
                .andExpect(jsonPath("$.qualityMessage").value("人工标记：手部关键点缺失"));
    }

    /**
     * 测试：PUT /sign/samples/{id}/quality 非法质量状态时返回 400。
     */
    @Test
    void updateQuality_shouldReturnBadRequest_whenQualityStatusInvalid() throws Exception {
        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("INVALID");
        request.setQualityMessage("非法状态");

        Mockito.when(signSampleService.updateQuality(eq(1L), any(SignSampleQualityUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("不支持的质量状态：INVALID"));

        mockMvc.perform(put("/sign/samples/1/quality")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("不支持的质量状态：INVALID"));
    }

    /**
     * 构造测试用样本实体。
     *
     * @param id            样本 ID
     * @param sampleCode    样本编码
     * @param resourceCode  资源编码
     * @param qualityStatus 质量状态
     * @param deleted       是否删除
     * @return 样本实体
     */
    private SignSample buildSample(
            Long id,
            String sampleCode,
            String resourceCode,
            String qualityStatus,
            Boolean deleted) {
        SignSample sample = new SignSample();

        sample.setId(id);
        sample.setSampleCode(sampleCode);
        sample.setResourceCode(resourceCode);
        sample.setLabel(resourceCode);
        sample.setRawFilePath("dataset/raw/" + resourceCode + "/" + sampleCode + ".json");
        sample.setFrameCount(30);
        sample.setDurationMs(4200);
        sample.setFps(new BigDecimal("7.14"));
        sample.setHandPresentRatio(new BigDecimal("0.9667"));
        sample.setPosePresentRatio(new BigDecimal("0.9000"));
        sample.setPoseNormalized(true);
        sample.setQualityStatus(qualityStatus);
        sample.setQualityMessage("测试质量说明");
        sample.setDeleted(deleted);

        return sample;
    }

    /**
     * 测试：POST /sign/samples/sync 从 Python 服务同步 raw 样本。
     */
    @Test
    void syncSamples_shouldReturnSyncResult() throws Exception {
        SignSampleSyncResult result = new SignSampleSyncResult(
                10,
                3,
                7,
                0,
                1
        );

        Mockito.when(signSampleService.syncFromPythonRawDataset()).thenReturn(result);

        mockMvc.perform(post("/sign/samples/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scannedCount").value(10))
                .andExpect(jsonPath("$.insertedCount").value(3))
                .andExpect(jsonPath("$.updatedCount").value(7))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.badCount").value(1));

        Mockito.verify(signSampleService).syncFromPythonRawDataset();
    }/**
     * 测试：POST /sign/samples/convert-features 执行 raw → feature 转换。
     */
    @Test
    void convertRawToFeatures_shouldReturnConvertResult() throws Exception {
        FeatureConvertResult result = new FeatureConvertResult();
        result.setRawRoot("dataset_raw_phone_10fps");
        result.setFeatureRoot("data_processed_arm_pose_10fps");
        result.setScannedCount(10);
        result.setConvertedCount(10);
        result.setSkippedCount(0);
        result.setFailedCount(0);
        result.setMessage("raw → feature 转换完成");

        Mockito.when(signSampleService.convertRawToFeatures()).thenReturn(result);

        mockMvc.perform(post("/sign/samples/convert-features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawRoot").value("dataset_raw_phone_10fps"))
                .andExpect(jsonPath("$.featureRoot").value("data_processed_arm_pose_10fps"))
                .andExpect(jsonPath("$.scannedCount").value(10))
                .andExpect(jsonPath("$.convertedCount").value(10))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.message").value("raw → feature 转换完成"));

        Mockito.verify(signSampleService).convertRawToFeatures();
    }



}
