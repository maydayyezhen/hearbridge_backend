package com.yezhen.hearbridge.backend.service;

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

    public SignCategoryService(SignCategoryMapper signCategoryMapper) {
        this.signCategoryMapper = signCategoryMapper;
    }

    /**
     * 查询全部分类。
     *
     * @return 分类列表
     */
    public List<SignCategory> listAll() {
        return signCategoryMapper.selectAll();
    }

    /**
     * 根据分类编码查询单个分类。
     *
     * @param code 分类编码
     * @return 分类信息
     */
    public SignCategory getByCode(String code) {
        return signCategoryMapper.selectByCode(code);
    }
}
