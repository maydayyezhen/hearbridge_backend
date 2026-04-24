package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.mapper.SignResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 手语资源 Service。
 */
@Service
public class SignResourceService {

    private final SignResourceMapper signResourceMapper;
    private final MinioProperties minioProperties;

    public SignResourceService(
            SignResourceMapper signResourceMapper,
            MinioProperties minioProperties) {
        this.signResourceMapper = signResourceMapper;
        this.minioProperties = minioProperties;
    }

    /**
     * 查询资源列表，支持按分类编码筛选。
     *
     * @param categoryCode 分类编码
     * @return 资源列表
     */
    public List<SignResource> list(String categoryCode) {
        List<SignResource> resources;
        if (!StringUtils.hasText(categoryCode)) {
            resources = signResourceMapper.selectAll();
        } else {
            resources = signResourceMapper.selectByCategoryCode(categoryCode);
        }
        resources.forEach(this::fillUrls);
        return resources;
    }

    /**
     * 根据资源编码查询单个资源。
     *
     * @param code 资源编码
     * @return 资源详情
     */
    public SignResource getByCode(String code) {
        SignResource resource = signResourceMapper.selectByCode(code);
        fillUrls(resource);
        return resource;
    }

    private void fillUrls(SignResource resource) {
        if (resource == null) {
            return;
        }
        resource.setSigmlUrl(minioProperties.buildObjectUrl(resource.getSigmlObjectKey()));
        resource.setCoverUrl(minioProperties.buildObjectUrl(resource.getCoverObjectKey()));
    }
}
