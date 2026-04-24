package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.mapper.SignCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 手语资源分类 Service。
 */
@Service
public class SignCategoryService {

    private final SignCategoryMapper signCategoryMapper;
    private final MinioProperties minioProperties;

    public SignCategoryService(
            SignCategoryMapper signCategoryMapper,
            MinioProperties minioProperties) {
        this.signCategoryMapper = signCategoryMapper;
        this.minioProperties = minioProperties;
    }

    /**
     * 查询全部分类。
     *
     * @return 分类列表
     */
    public List<SignCategory> listAll() {
        List<SignCategory> categories = signCategoryMapper.selectAll();
        categories.forEach(this::fillUrls);
        return categories;
    }

    /**
     * 根据分类编码查询单个分类。
     *
     * @param code 分类编码
     * @return 分类信息
     */
    public SignCategory getByCode(String code) {
        SignCategory category = signCategoryMapper.selectByCode(code);
        fillUrls(category);
        return category;
    }

    private void fillUrls(SignCategory category) {
        if (category == null) {
            return;
        }
        category.setCoverUrl(minioProperties.buildObjectUrl(category.getCoverObjectKey()));
    }
}
