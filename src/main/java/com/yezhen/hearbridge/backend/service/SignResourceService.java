package com.yezhen.hearbridge.backend.service;

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

    public SignResourceService(SignResourceMapper signResourceMapper) {
        this.signResourceMapper = signResourceMapper;
    }

    /**
     * 查询资源列表，支持按分类编码筛选。
     *
     * @param categoryCode 分类编码
     * @return 资源列表
     */
    public List<SignResource> list(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return signResourceMapper.selectAll();
        }
        return signResourceMapper.selectByCategoryCode(categoryCode);
    }

    /**
     * 根据资源编码查询单个资源。
     *
     * @param code 资源编码
     * @return 资源详情
     */
    public SignResource getByCode(String code) {
        return signResourceMapper.selectByCode(code);
    }
}
