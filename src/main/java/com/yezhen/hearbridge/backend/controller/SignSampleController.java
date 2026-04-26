package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.SignSamplePageResult;
import com.yezhen.hearbridge.backend.dto.SignSampleQualityUpdateRequest;
import com.yezhen.hearbridge.backend.dto.SignSampleQuery;
import com.yezhen.hearbridge.backend.dto.SignSampleSummary;
import com.yezhen.hearbridge.backend.entity.SignSample;
import com.yezhen.hearbridge.backend.service.SignSampleService;
import org.springframework.web.bind.annotation.*;
import com.yezhen.hearbridge.backend.dto.SignSampleSyncResult;
import com.yezhen.hearbridge.backend.dto.FeatureConvertResult;
import com.yezhen.hearbridge.backend.dto.ModelTrainResult;

/**
 * 手势样本管理 Controller。
 */
@RestController
@RequestMapping("/sign/samples")
public class SignSampleController {

    /**
     * 手势样本 Service。
     */
    private final SignSampleService signSampleService;

    /**
     * 构造注入手势样本 Service。
     *
     * @param signSampleService 手势样本 Service
     */
    public SignSampleController(SignSampleService signSampleService) {
        this.signSampleService = signSampleService;
    }

    /**
     * 分页查询样本列表。
     *
     * @param query 查询参数
     * @return 分页样本列表
     */
    @GetMapping
    public SignSamplePageResult listSamples(SignSampleQuery query) {
        return signSampleService.list(query);
    }

    /**
     * 查询样本统计信息。
     *
     * @return 样本统计信息
     */
    @GetMapping("/summary")
    public SignSampleSummary getSummary() {
        return signSampleService.summary();
    }

    /**
     * 从 Python 服务同步 raw 样本摘要。
     *
     * @return 同步结果
     */
    @PostMapping("/sync")
    public SignSampleSyncResult syncSamples() {
        return signSampleService.syncFromPythonRawDataset();
    }

    /**
     * 软删除样本。
     *
     * @param id 样本 ID
     */
    @DeleteMapping("/{id}")
    public void deleteSample(@PathVariable("id") Long id) {
        signSampleService.deleteById(id);
    }

    /**
     * 更新样本质量状态。
     *
     * @param id      样本 ID
     * @param request 质量更新请求
     * @return 更新后的样本信息
     */
    @PutMapping("/{id}/quality")
    public SignSample updateQuality(
            @PathVariable("id") Long id,
            @RequestBody SignSampleQualityUpdateRequest request) {
        return signSampleService.updateQuality(id, request);
    }

    /**
     * 调用 Python 服务执行 raw → feature 转换。
     *
     * @return 转换结果
     */
    @PostMapping("/convert-features")
    public FeatureConvertResult convertRawToFeatures() {
        return signSampleService.convertRawToFeatures();
    }

    /**
     * 调用 Python 服务执行模型训练。
     *
     * @return 模型训练结果
     */
    @PostMapping("/train")
    public ModelTrainResult trainModel() {
        return signSampleService.trainModel();
    }

}
