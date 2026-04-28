package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.dto.PageResult;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.mapper.SignCategoryMapper;
import com.yezhen.hearbridge.backend.mapper.SignResourceMapper;
import com.yezhen.hearbridge.backend.util.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 手语资源分类 Service。
 *
 * 职责边界：
 * 1. Controller 只负责接收请求。
 * 2. Mapper 只负责数据库访问。
 * 3. Service 负责业务校验、补全 URL、组织 CRUD 流程。
 */
@Service
public class SignCategoryService {

    /**
     * 手语资源分类 Mapper。
     */
    private final SignCategoryMapper signCategoryMapper;

    /**
     * 手语资源 Mapper。
     *
     * 用于在删除分类、修改分类编码前检查资源引用关系。
     */
    private final SignResourceMapper signResourceMapper;

    /**
     * MinIO 配置工具。
     *
     * 用于根据 objectKey 生成前端 / 手机端可访问的完整 URL。
     */
    private final MinioProperties minioProperties;

    /**
     * 构造注入依赖。
     *
     * @param signCategoryMapper 手语资源分类 Mapper
     * @param signResourceMapper 手语资源 Mapper
     * @param minioProperties    MinIO 配置工具
     */
    public SignCategoryService(
            SignCategoryMapper signCategoryMapper,
            SignResourceMapper signResourceMapper,
            MinioProperties minioProperties) {
        this.signCategoryMapper = signCategoryMapper;
        this.signResourceMapper = signResourceMapper;
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
     * 分页查询分类。
     *
     * @param pageNo 当前页码
     * @param pageSize 每页数量
     * @return 分类分页结果
     */
    public PageResult<SignCategory> page(Integer pageNo, Integer pageSize) {
        int safePageNo = PageUtils.normalizePageNo(pageNo);
        int safePageSize = PageUtils.normalizePageSize(pageSize);
        int offset = (safePageNo - 1) * safePageSize;

        long total = signCategoryMapper.countAll();
        List<SignCategory> records = signCategoryMapper.selectPage(offset, safePageSize);
        records.forEach(this::fillUrls);

        return PageResult.of(records, total, safePageNo, safePageSize);
    }

    /**
     * 搜索分类总数。
     *
     * @param keyword 搜索关键词
     * @return 匹配分类总数
     */
    public long countByKeyword(String keyword) {
        return signCategoryMapper.countByKeyword(keyword);
    }

    /**
     * 分页搜索分类。
     *
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 当前页分类列表
     */
    public List<SignCategory> searchPage(String keyword, int offset, int limit) {
        List<SignCategory> records = signCategoryMapper.searchPage(keyword, offset, limit);
        records.forEach(this::fillUrls);
        return records;
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

    /**
     * 根据主键 ID 查询单个分类。
     *
     * @param id 分类主键 ID
     * @return 分类信息
     */
    public SignCategory getById(Long id) {
        SignCategory category = signCategoryMapper.selectById(id);
        fillUrls(category);
        return category;
    }

    /**
     * 新增手语资源分类。
     *
     * @param category 分类信息
     * @return 新增后的分类信息
     */
    public SignCategory create(SignCategory category) {
        validateForSave(category);

        SignCategory existed = signCategoryMapper.selectByCode(category.getCode());
        if (existed != null) {
            throw new IllegalArgumentException("分类编码已存在：" + category.getCode());
        }

        signCategoryMapper.insert(category);
        return getById(category.getId());
    }

    /**
     * 更新手语资源分类。
     *
     * 保护规则：
     * 1. 分类不存在：不允许更新。
     * 2. 新 code 和其他分类冲突：不允许更新。
     * 3. 当前分类下已有资源时：不允许修改 code，避免 sign_resource.category_code 变成孤儿引用。
     * 4. 当前分类下已有资源时：允许修改 nameZh / coverObjectKey。
     *
     * @param id       分类主键 ID
     * @param category 分类更新信息
     * @return 更新后的分类信息
     */
    public SignCategory update(Long id, SignCategory category) {
        if (id == null) {
            throw new IllegalArgumentException("分类 ID 不能为空");
        }

        validateForSave(category);

        SignCategory oldCategory = signCategoryMapper.selectById(id);
        if (oldCategory == null) {
            throw new IllegalArgumentException("分类不存在，ID：" + id);
        }

        SignCategory sameCodeCategory = signCategoryMapper.selectByCode(category.getCode());
        if (sameCodeCategory != null && !sameCodeCategory.getId().equals(id)) {
            throw new IllegalArgumentException("分类编码已存在：" + category.getCode());
        }

        boolean codeChanged = !oldCategory.getCode().equals(category.getCode());
        if (codeChanged) {
            int resourceCount = signResourceMapper.countByCategoryCode(oldCategory.getCode());
            if (resourceCount > 0) {
                throw new IllegalArgumentException(
                        "当前分类下仍有手势资源，不能修改分类编码：" +
                                oldCategory.getNameZh() +
                                "（" +
                                oldCategory.getCode() +
                                "）"
                );
            }
        }

        category.setId(id);
        signCategoryMapper.updateById(category);
        return getById(id);
    }

    /**
     * 根据主键 ID 删除分类。
     *
     * 保护规则：
     * 1. 分类不存在：不允许删除。
     * 2. 分类下仍有资源：不允许删除。
     * 3. 分类下没有资源：允许删除。
     *
     * @param id 分类主键 ID
     */
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分类 ID 不能为空");
        }

        SignCategory oldCategory = signCategoryMapper.selectById(id);
        if (oldCategory == null) {
            throw new IllegalArgumentException("分类不存在，ID：" + id);
        }

        int resourceCount = signResourceMapper.countByCategoryCode(oldCategory.getCode());
        if (resourceCount > 0) {
            throw new IllegalArgumentException(
                    "当前分类下仍有手势资源，不能删除：" +
                            oldCategory.getNameZh() +
                            "（" +
                            oldCategory.getCode() +
                            "）"
            );
        }

        signCategoryMapper.deleteById(id);
    }

    /**
     * 校验保存分类时的核心字段。
     *
     * @param category 分类信息
     */
    private void validateForSave(SignCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("分类信息不能为空");
        }

        if (!StringUtils.hasText(category.getCode())) {
            throw new IllegalArgumentException("分类编码不能为空");
        }

        if (!StringUtils.hasText(category.getNameZh())) {
            throw new IllegalArgumentException("分类中文名称不能为空");
        }
    }

    /**
     * 根据 objectKey 补全可访问 URL。
     *
     * @param category 分类信息
     */
    private void fillUrls(SignCategory category) {
        if (category == null) {
            return;
        }

        category.setCoverUrl(minioProperties.buildObjectUrl(category.getCoverObjectKey()));
    }
}
