package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleItem;
import com.yezhen.hearbridge.backend.dto.PythonRawSampleListResponse;
import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.mapper.SignSampleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 手势样本 Service 基础白盒测试。
 *
 * 训练产物 MinIO 接管流程单独由 SignSampleArtifactFlowTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class SignSampleServiceTest {

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
     * 测试：查询样本列表时，如果没有传分页参数，应使用默认 page=1、pageSize=20。
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
        assertEquals("sample_a_001", result.getItems().get(0).getSampleCode());
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
        when(signSampleMapper.updateQuality(1L, "BAD", "人工标记：手部关键点缺失")).thenReturn(1);

        SignSample result = signSampleService.updateQuality(1L, request);

        assertEquals("BAD", result.getQualityStatus());
        assertEquals("人工标记：手部关键点缺失", result.getQualityMessage());
        verify(signSampleMapper).updateQuality(1L, "BAD", "人工标记：手部关键点缺失");
    }

    /**
     * 测试：更新样本质量状态时，如果状态非法，应抛出异常。
     */
    @Test
    void updateQuality_shouldThrowException_whenQualityStatusInvalid() {
        SignSampleQualityUpdateRequest request = new SignSampleQualityUpdateRequest();
        request.setQualityStatus("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signSampleService.updateQuality(1L, request)
        );

        assertEquals("不支持的质量状态：INVALID", exception.getMessage());
        verify(signSampleMapper, never()).selectById(anyLong());
        verify(signSampleMapper, never()).updateQuality(anyLong(), anyString(), anyString());
    }

    /**
     * 测试：从 Python 服务同步 raw 样本时，新样本应插入数据库。
     */
    @Test
    void syncFromPythonRawDataset_shouldInsertSamples_whenSamplesNotExist() {
        PythonRawSampleItem item = buildPythonRawSampleItem("a_sample_001", "a", "GOOD");
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
        verify(signSampleMapper).insert(any(SignSample.class));
        verify(signSampleMapper, never()).updateBySampleCode(any());
    }

    /**
     * 测试：调用 Python 服务执行 raw → feature 转换成功。
     */
    @Test
    void convertRawToFeatures_shouldReturnResult_whenPythonServiceReturnsResult() {
        FeatureConvertResult result = new FeatureConvertResult();
        result.setScannedCount(10);
        result.setConvertedCount(10);
        result.setFailedCount(0);
        result.setMessage("raw → feature 转换完成");

        when(pythonGestureServiceClient.convertRawToFeatures()).thenReturn(result);

        FeatureConvertResult actual = signSampleService.convertRawToFeatures();

        assertEquals(10, actual.getScannedCount());
        assertEquals(10, actual.getConvertedCount());
        assertEquals("raw → feature 转换完成", actual.getMessage());
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
    }

    private SignSample buildSample(Long id, String sampleCode, String resourceCode, String qualityStatus, Boolean deleted) {
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

    private PythonRawSampleItem buildPythonRawSampleItem(String sampleCode, String resourceCode, String qualityStatus) {
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
}
