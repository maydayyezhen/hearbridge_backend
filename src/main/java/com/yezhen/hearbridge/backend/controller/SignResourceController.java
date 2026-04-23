package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.service.SignResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 手语资源 Controller。
 */
@RestController
@RequestMapping("/sign/resources")
public class SignResourceController {

    private final SignResourceService signResourceService;

    public SignResourceController(SignResourceService signResourceService) {
        this.signResourceService = signResourceService;
    }

    /**
     * 查询资源列表，支持按分类编码筛选。
     *
     * @param categoryCode 分类编码
     * @return 资源列表
     */
    @GetMapping
    public List<SignResource> listResources(
            @RequestParam(value = "categoryCode", required = false) String categoryCode) {
        return signResourceService.list(categoryCode);
    }

    /**
     * 根据资源编码查询资源详情。
     *
     * @param code 资源编码
     * @return 资源详情
     */
    @GetMapping("/{code}")
    public SignResource getResource(@PathVariable("code") String code) {
        return signResourceService.getByCode(code);
    }
}
