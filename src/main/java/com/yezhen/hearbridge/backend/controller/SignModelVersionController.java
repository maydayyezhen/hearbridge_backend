package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignModelVersion;
import com.yezhen.hearbridge.backend.service.SignModelVersionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型版本管理 Controller。
 */
@RestController
@RequestMapping("/sign/model-versions")
public class SignModelVersionController {

    /**
     * 模型版本 Service。
     */
    private final SignModelVersionService signModelVersionService;

    /**
     * 构造注入模型版本 Service。
     *
     * @param signModelVersionService 模型版本 Service
     */
    public SignModelVersionController(SignModelVersionService signModelVersionService) {
        this.signModelVersionService = signModelVersionService;
    }

    /**
     * 查询模型版本列表。
     *
     * @return 模型版本列表
     */
    @GetMapping
    public List<SignModelVersion> listModelVersions() {
        return signModelVersionService.listAll();
    }

    /**
     * 查询当前发布版本。
     *
     * @return 当前发布版本
     */
    @GetMapping("/published")
    public SignModelVersion getPublishedVersion() {
        return signModelVersionService.getPublished();
    }

    /**
     * 发布指定模型版本。
     *
     * @param id 模型版本 ID
     * @return 发布后的模型版本
     */
    @PutMapping("/{id}/publish")
    public SignModelVersion publishModelVersion(@PathVariable("id") Long id) {
        return signModelVersionService.publish(id);
    }
}
