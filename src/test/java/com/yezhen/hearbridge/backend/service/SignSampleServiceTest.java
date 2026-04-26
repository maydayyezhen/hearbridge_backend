package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleItem;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 手势样本 Service 白盒测试。
 *
 * 测试重点：
 * 1. 分页参数归一化；
 * 2. 样本统计；
 * 3. 软删除样本；
 * 4. 样本质量状态更新；
 * 5. 非法质量状态校验。
 */
@ExtendWith(MockitoExtension.class)
class SignSampleServiceTest {

    /**
     * 手势样本 Mapper Mock。
     */
    @Mock
    private SignSampleMapper signSampleMapper;

    /**
     * Python 手势识别服务客户端 Mock。
     */
    @Mock
    private PythonGestureServiceClient pythonGestureServiceClient;

    /**
     * 模型版本 Service Mock。
     */
    @Mock
    private SignModelVersionService signModelVersionService;

    /**
     * 被测试的手势样本 Service。
     */
    private SignSampleService signSampleService;

    /**
     * 每个测试用例执行前初始化 Service。
     */
    @BeforeEach
    void setUp() {
        signSampleService = new SignSampleService(
                signSampleMapper,
                pythonGestureServiceClient,
                signModelVersionService
        );
    }

    /**
     * 测试：查询样本列表时，如果没有传分页参数，应使用默认 page=1、pageSize=20、deleted=false。
     */
    @Test
    void list_shouldUseDefaultQuery_whenQueryIsEmpty() {
        SignSample sample = buildSample(1L, "sample_a_001", "a", "GOOD", false);

        when(signSampleMapper.countPage(any(SignSampleQuery.class))).thenReturn(1L);
        when(signSampleMapper.selectPage(any(SignSampleQuery.class), eq(0), eq(20)))
                .thenReturn(List.of(sample));

        SignSamplePageResult result = signSampleService.list(new SignSampleQuery());

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getPageSize());
        assertEquals(1, result.getItems().size());
        assertEquals("sample_a_001", result.getItems().get(0).getSampleCode());

        verify(signSampleMapper).countPage(any(SignSampleQuery.class));
        verify(signSampleMapper).selectPage(any(SignSampleQuery.class), eq(0), eq(20));
    }

    /**
     * 测试：查询样本列表时，页码和每页数量应正确计算 offset。
     */
    @Test
    void list_shouldCalculateOffset_whenPageAndPageSizeProvided() {
        SignSampleQuery query = new SignSampleQuery();
        query.setPage(3);
        query.setPageSize(10);

        when(signSampleMapper.countPage(query)).thenReturn(0L);
        when(signSampleMapper.selectPage(query, 20, 10)).thenReturn(List.of());

        SignSamplePageResult result = signSampleService.list(query);

        assertEquals(3, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(0L, result.getTotal());

        verify(signSampleMapper).selectPage(query, 20, 10);
    }

    /**
     * 测试：pageSize 超过最大值时，应限制为 100。
     */
    @Test
    void list_shouldLimitPageSize_whenPageSizeTooLarge() {
        SignSampleQuery query = new SignSampleQuery();
        query.setPage(1);
        query.setPageSize(999);

        when(signSampleMapper.countPage(query)).thenReturn(0L);
        when(signSampleMapper.selectPage(query, 0, 100)).thenReturn(List.of());

        SignSamplePageResult result = signSampleService.list(query);

        assertEquals(100, result.getPageSize());

        verify(signSampleMapper).selectPage(query, 0, 100);
    }

    /**
     * 测试：查询时传入非法质量状态，应抛出异常。
     */
    @Test
    void list_shouldThrowException_whenQualityStatusInvalid() {
        SignSampleQuery query = new SignSampleQuery();
        query.setQualityStatus("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.list(query)
        );

        assertEquals("不支持的质量状态：INVALID", exception.getMessage());

        verify(signSampleMapper, never()).countPage(any());
        verify(signSampleMapper, never()).selectPage(any(), anyInt(), anyInt());
    }

    /**
     * 测试：查询样本统计信息成功。
     */
    @Test
    void summary_shouldReturnSummary() {
        SignSampleSummary summary = new SignSampleSummary(
                10L,
                3L,
                6L,
                2L,
                1L,
                1L
        );

        when(signSampleMapper.selectSummary()).thenReturn(summary);

        SignSampleSummary result = signSampleService.summary();

        assertEquals(10L, result.getTotalCount());
        assertEquals(3L, result.getResourceCount());
        assertEquals(6L, result.getGoodCount());
        assertEquals(2L, result.getWarningCount());
        assertEquals(1L, result.getBadCount());
        assertEquals(1L, result.getUnknownCount());

        verify(signSampleMapper).selectSummary();
    }

    /**
     * 测试：统计结果为空时，应返回全 0 统计。
     */
    @Test
    void summary_shouldReturnZeroSummary_whenMapperReturnsNull() {
        when(signSampleMapper.selectSummary()).thenReturn(null);

        SignSampleSummary result = signSampleService.summary();

        assertEquals(0L, result.getTotalCount());
        assertEquals(0L, result.getResourceCount());
        assertEquals(0L, result.getGoodCount());
        assertEquals(0L, result.getWarningCount());
        assertEquals(0L, result.getBadCount());
        assertEquals(0L, result.getUnknownCount());
    }

    /**
     * 测试：软删除未删除样本成功。
     */
    @Test
    void deleteById_shouldSoftDeleteSample_whenSampleExists() {
        SignSample sample = buildSample(1L, "sample_a_001", "a", "GOOD", false);

        when(signSampleMapper.selectById(1L)).thenReturn(sample);
        when(signSampleMapper.softDeleteById(1L)).thenReturn(1);

        signSampleService.deleteById(1L);

        verify(signSampleMapper).selectById(1L);
        verify(signSampleMapper).softDeleteById(1L);
    }

    /**
     * 测试：删除不存在的样本时，应抛出异常。
     */
    @Test
    void deleteById_shouldThrowException_whenSampleNotExists() {
        when(signSampleMapper.selectById(404L)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.deleteById(404L)
        );

        assertEquals("样本不存在，ID：404", exception.getMessage());

        verify(signSampleMapper).selectById(404L);
        verify(signSampleMapper, never()).softDeleteById(anyLong());
    }

    /**
     * 测试：删除已删除样本时，应抛出异常。
     */
    @Test
    void deleteById_shouldThrowException_whenSampleAlreadyDeleted() {
        SignSample sample = buildSample(1L, "sample_a_001", "a", "GOOD", true);

        when(signSampleMapper.selectById(1L)).thenReturn(sample);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.deleteById(1L)
        );

        assertEquals("样本已删除，ID：1", exception.getMessage());

        verify(signSampleMapper).selectById(1L);
        verify(signSampleMapper, never()).softDeleteById(anyLong());
    }

    /**
     * 测试：更新样本质量状态成功。
     */
    @Test
    void updateQuality_shouldUpdateQuality_whenRequestValid() {
        SignSample oldSample = buildSample(1L, "sample_a_001", "a", "GOOD", false);
        SignSample updatedSample = buildSample(1L, "sample_a_001", "a", "BAD", false);
        updatedSample.setQualityMessage("人工标记：手部关键点缺失");

        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("BAD");
        request.setQualityMessage("人工标记：手部关键点缺失");

        when(signSampleMapper.selectById(1L)).thenReturn(oldSample, updatedSample);
        when(signSampleMapper.updateQuality(1L, "BAD", "人工标记：手部关键点缺失"))
                .thenReturn(1);

        SignSample result = signSampleService.updateQuality(1L, request);

        assertEquals("BAD", result.getQualityStatus());
        assertEquals("人工标记：手部关键点缺失", result.getQualityMessage());

        verify(signSampleMapper, times(2)).selectById(1L);
        verify(signSampleMapper).updateQuality(1L, "BAD", "人工标记：手部关键点缺失");
    }

    /**
     * 测试：更新样本质量状态时，如果状态非法，应抛出异常。
     */
    @Test
    void updateQuality_shouldThrowException_whenQualityStatusInvalid() {
        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("INVALID");
        request.setQualityMessage("非法状态");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.updateQuality(1L, request)
        );

        assertEquals("不支持的质量状态：INVALID", exception.getMessage());

        verify(signSampleMapper, never()).selectById(anyLong());
        verify(signSampleMapper, never()).updateQuality(anyLong(), anyString(), anyString());
    }

    /**
     * 测试：更新已删除样本的质量状态时，应抛出异常。
     */
    @Test
    void updateQuality_shouldThrowException_whenSampleDeleted() {
        SignSample deletedSample = buildSample(1L, "sample_a_001", "a", "GOOD", true);

        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("BAD");
        request.setQualityMessage("人工标记");

        when(signSampleMapper.selectById(1L)).thenReturn(deletedSample);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.updateQuality(1L, request)
        );

        assertEquals("样本已删除，不能修改质量状态，ID：1", exception.getMessage());

        verify(signSampleMapper).selectById(1L);
        verify(signSampleMapper, never()).updateQuality(anyLong(), anyString(), anyString());
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
     * 测试：从 Python 服务同步 raw 样本时，新样本应插入数据库。
     */
    @Test
    void syncFromPythonRawDataset_shouldInsertSamples_whenSamplesNotExist() {
        PythonRawSampleItem item = buildPythonRawSampleItem(
                "a_sample_001",
                "a",
                "GOOD"
        );

        PythonRawSampleListResponse response = new PythonRawSampleListResponse();
        response.setRootDir("dataset_raw_phone_10fps");
        response.setTotal(1);
        response.setItems(List.of(item));

        when(pythonGestureServiceClient.listRawSamples()).thenReturn(response);
        when(signSampleMapper.selectBySampleCode("a_sample_001")).thenReturn(null);
        when(signSampleMapper.insert(any(SignSample.class))).thenReturn(1);

        SignSampleSyncResult result = signSampleService.syncFromPythonRawDataset();

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getInsertedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getBadCount());

        verify(pythonGestureServiceClient).listRawSamples();
        verify(signSampleMapper).selectBySampleCode("a_sample_001");
        verify(signSampleMapper).insert(any(SignSample.class));
        verify(signSampleMapper, never()).updateBySampleCode(any());
    }

    /**
     * 测试：从 Python 服务同步 raw 样本时，已存在样本应更新数据库。
     */
    @Test
    void syncFromPythonRawDataset_shouldUpdateSamples_whenSamplesExist() {
        PythonRawSampleItem item = buildPythonRawSampleItem(
                "a_sample_001",
                "a",
                "BAD"
        );

        PythonRawSampleListResponse response = new PythonRawSampleListResponse();
        response.setRootDir("dataset_raw_phone_10fps");
        response.setTotal(1);
        response.setItems(List.of(item));

        SignSample existed = buildSample(1L, "a_sample_001", "a", "GOOD", false);

        when(pythonGestureServiceClient.listRawSamples()).thenReturn(response);
        when(signSampleMapper.selectBySampleCode("a_sample_001")).thenReturn(existed);
        when(signSampleMapper.updateBySampleCode(any(SignSample.class))).thenReturn(1);

        SignSampleSyncResult result = signSampleService.syncFromPythonRawDataset();

        assertEquals(1, result.getScannedCount());
        assertEquals(0, result.getInsertedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(1, result.getBadCount());

        verify(pythonGestureServiceClient).listRawSamples();
        verify(signSampleMapper).selectBySampleCode("a_sample_001");
        verify(signSampleMapper).updateBySampleCode(any(SignSample.class));
        verify(signSampleMapper, never()).insert(any());
    }

    /**
     * 构造 Python raw 样本摘要测试对象。
     *
     * @param sampleCode    样本编码
     * @param resourceCode  资源编码
     * @param qualityStatus 质量状态
     * @return Python raw 样本摘要
     */
    private PythonRawSampleItem buildPythonRawSampleItem(
            String sampleCode,
            String resourceCode,
            String qualityStatus) {
        PythonRawSampleItem item = new PythonRawSampleItem();

        item.setSampleCode(sampleCode);
        item.setResourceCode(resourceCode);
        item.setLabel(resourceCode);
        item.setRawFilePath("dataset_raw_phone_10fps/" + resourceCode + "/sample_001.npz");
        item.setFrameCount(30);
        item.setDurationMs(2900);
        item.setFps(new BigDecimal("10.34"));
        item.setHandPresentRatio(new BigDecimal("1.0000"));
        item.setPosePresentRatio(new BigDecimal("0.9000"));
        item.setPoseNormalized(true);
        item.setQualityStatus(qualityStatus);
        item.setQualityMessage("样本质量测试");

        return item;
    }

    /**
     * 测试：调用 Python 服务执行 raw → feature 转换成功。
     */
    @Test
    void convertRawToFeatures_shouldReturnResult_whenPythonServiceReturnsResult() {
        FeatureConvertResult result = new FeatureConvertResult();
        result.setRawRoot("dataset_raw_phone_10fps");
        result.setFeatureRoot("data_processed_arm_pose_10fps");
        result.setScannedCount(10);
        result.setConvertedCount(10);
        result.setSkippedCount(0);
        result.setFailedCount(0);
        result.setMessage("raw → feature 转换完成");

        when(pythonGestureServiceClient.convertRawToFeatures()).thenReturn(result);

        FeatureConvertResult actual = signSampleService.convertRawToFeatures();

        assertEquals(10, actual.getScannedCount());
        assertEquals(10, actual.getConvertedCount());
        assertEquals(0, actual.getFailedCount());
        assertEquals("raw → feature 转换完成", actual.getMessage());

        verify(pythonGestureServiceClient).convertRawToFeatures();
    }

    /**
     * 测试：Python 服务未返回转换结果时，应抛出异常。
     */
    @Test
    void convertRawToFeatures_shouldThrowException_whenPythonServiceReturnsNull() {
        when(pythonGestureServiceClient.convertRawToFeatures()).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.convertRawToFeatures()
        );

        assertEquals("raw → feature 转换失败：Python 服务未返回结果", exception.getMessage());

        verify(pythonGestureServiceClient).convertRawToFeatures();
    }

    /**
     * 测试：调用 Python 服务执行模型训练成功，并登记模型版本。
     */
    @Test
    void trainModel_shouldReturnResultAndCreateModelVersion_whenPythonServiceReturnsResult() {
        ModelTrainResult result = new ModelTrainResult();
        result.setRunName("train_20260426_153000");
        result.setSampleCount(80);
        result.setTrainSampleCount(64);
        result.setValSampleCount(16);
        result.setClassCount(10);
        result.setEpochsRan(20);
        result.setMessage("training completed");

        SignModelVersion version = new SignModelVersion();
        version.setId(1L);
        version.setVersionName("train_20260426_153000");
        version.setStatus("TRAINED");
        version.setPublished(false);

        when(pythonGestureServiceClient.trainModel()).thenReturn(result);
        when(signModelVersionService.createFromTrainResult(result)).thenReturn(version);

        ModelTrainResult actual = signSampleService.trainModel();

        assertEquals("train_20260426_153000", actual.getRunName());
        assertEquals(80, actual.getSampleCount());
        assertEquals(10, actual.getClassCount());
        assertEquals("training completed", actual.getMessage());
        assertEquals(1L, actual.getVersionId());
        assertEquals("train_20260426_153000", actual.getVersionName());
        assertEquals("TRAINED", actual.getVersionStatus());
        assertFalse(actual.getPublished());

        verify(pythonGestureServiceClient).trainModel();
        verify(signModelVersionService).createFromTrainResult(result);
    }

    /**
     * 测试：Python 服务未返回训练结果时，应抛出异常。
     */
    @Test
    void trainModel_shouldThrowException_whenPythonServiceReturnsNull() {
        when(pythonGestureServiceClient.trainModel()).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.trainModel()
        );

        assertEquals("模型训练失败：Python 服务未返回结果", exception.getMessage());

        verify(pythonGestureServiceClient).trainModel();
    }

}
