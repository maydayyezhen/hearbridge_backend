package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.PageResult;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.service.SignResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手语资源 Controller。
 */
@RestController
@RequestMapping("/sign/resources")
public class SignResourceController {

    /**
     * 手语资源业务服务。
     */
    private final SignResourceService signResourceService;

    /**
     * 构造注入手语资源业务服务。
     *
     * @param signResourceService 手语资源业务服务
     */
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
     * 分页查询资源列表，支持按分类编码筛选。
     *
     * @param categoryCode 分类编码
     * @param pageNo       当前页码，从 1 开始
     * @param pageSize     每页数量
     * @return 资源分页结果
     */
    @GetMapping("/page")
    public PageResult<SignResource> pageResources(
            @RequestParam(value = "categoryCode", required = false) String categoryCode,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return signResourceService.page(categoryCode, pageNo, pageSize);
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

    /**
     * 新增手语资源。
     *
     * @param resource 资源信息
     * @return 新增后的资源信息
     */
    @PostMapping
    public SignResource createResource(@RequestBody SignResource resource) {
        return signResourceService.create(resource);
    }

    /**
     * 更新手语资源。
     *
     * @param id       资源主键 ID
     * @param resource 资源更新信息
     * @return 更新后的资源信息
     */
    @PutMapping("/{id}")
    public SignResource updateResource(
            @PathVariable("id") Long id,
            @RequestBody SignResource resource) {
        return signResourceService.update(id, resource);
    }

    /**
     * 删除手语资源。
     *
     * @param id 资源主键 ID
     */
    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable("id") Long id) {
        signResourceService.deleteById(id);
    }
}
