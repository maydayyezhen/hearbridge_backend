package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.mapper.SignCategoryMapper;
import com.yezhen.hearbridge.backend.mapper.SignResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 手语资源 Service。
 *
 * 职责：
 * 1. 查询资源列表；
 * 2. 补全资源封面 URL 和 SiGML URL；
 * 3. 新增、编辑、删除手势资源；
 * 4. 校验资源编码唯一性；
 * 5. 校验资源所属分类是否存在。
 */
@Service
public class SignResourceService {

    /**
     * 手语资源 Mapper。
     */
    private final SignResourceMapper signResourceMapper;

    /**
     * 手语分类 Mapper。
     *
     * 用于校验资源所属分类是否存在。
     */
    private final SignCategoryMapper signCategoryMapper;

    /**
     * MinIO 配置工具。
     */
    private final MinioProperties minioProperties;

    /**
     * 构造注入依赖。
     *
     * @param signResourceMapper 手语资源 Mapper
     * @param signCategoryMapper 手语分类 Mapper
     * @param minioProperties    MinIO 配置工具
     */
    public SignResourceService(
            SignResourceMapper signResourceMapper,
            SignCategoryMapper signCategoryMapper,
            MinioProperties minioProperties) {
        this.signResourceMapper = signResourceMapper;
        this.signCategoryMapper = signCategoryMapper;
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

    /**
     * 根据主键 ID 查询单个资源。
     *
     * @param id 资源主键 ID
     * @return 资源详情
     */
    public SignResource getById(Long id) {
        SignResource resource = signResourceMapper.selectById(id);
        fillUrls(resource);
        return resource;
    }

    /**
     * 新增手语资源。
     *
     * @param resource 资源信息
     * @return 新增后的资源信息
     */
    public SignResource create(SignResource resource) {
        validateForSave(resource);

        SignResource existed = signResourceMapper.selectByCode(resource.getCode());
        if (existed != null) {
            throw new IllegalArgumentException("资源编码已存在：" + resource.getCode());
        }

        validateCategoryExists(resource.getCategoryCode());

        signResourceMapper.insert(resource);
        return getById(resource.getId());
    }

    /**
     * 更新手语资源。
     *
     * @param id       资源主键 ID
     * @param resource 资源更新信息
     * @return 更新后的资源信息
     */
    public SignResource update(Long id, SignResource resource) {
        if (id == null) {
            throw new IllegalArgumentException("资源 ID 不能为空");
        }

        validateForSave(resource);

        SignResource oldResource = signResourceMapper.selectById(id);
        if (oldResource == null) {
            throw new IllegalArgumentException("资源不存在，ID：" + id);
        }

        SignResource sameCodeResource = signResourceMapper.selectByCode(resource.getCode());
        if (sameCodeResource != null && !sameCodeResource.getId().equals(id)) {
            throw new IllegalArgumentException("资源编码已存在：" + resource.getCode());
        }

        validateCategoryExists(resource.getCategoryCode());

        resource.setId(id);
        signResourceMapper.updateById(resource);
        return getById(id);
    }

    /**
     * 根据主键 ID 删除资源。
     *
     * @param id 资源主键 ID
     */
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("资源 ID 不能为空");
        }

        SignResource oldResource = signResourceMapper.selectById(id);
        if (oldResource == null) {
            throw new IllegalArgumentException("资源不存在，ID：" + id);
        }

        signResourceMapper.deleteById(id);
    }

    /**
     * 校验保存资源时的核心字段。
     *
     * @param resource 资源信息
     */
    private void validateForSave(SignResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("资源信息不能为空");
        }

        if (!StringUtils.hasText(resource.getCode())) {
            throw new IllegalArgumentException("资源编码不能为空");
        }

        if (!StringUtils.hasText(resource.getNameZh())) {
            throw new IllegalArgumentException("资源中文名称不能为空");
        }

        if (!StringUtils.hasText(resource.getCategoryCode())) {
            throw new IllegalArgumentException("所属分类不能为空");
        }
    }

    /**
     * 校验分类是否存在。
     *
     * @param categoryCode 分类编码
     */
    private void validateCategoryExists(String categoryCode) {
        SignCategory category = signCategoryMapper.selectByCode(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException("所属分类不存在：" + categoryCode);
        }
    }

    /**
     * 补全资源可访问 URL。
     *
     * @param resource 资源信息
     */
    private void fillUrls(SignResource resource) {
        if (resource == null) {
            return;
        }

        resource.setSigmlUrl(minioProperties.buildObjectUrl(resource.getSigmlObjectKey()));
        resource.setCoverUrl(minioProperties.buildObjectUrl(resource.getCoverObjectKey()));
    }
}
