package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.mapper.SignCategoryMapper;
import com.yezhen.hearbridge.backend.mapper.SignResourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 手势分类 Service 白盒测试。
 *
 * 测试重点：
 * 1. 分类编码唯一性校验；
 * 2. 分类删除保护；
 * 3. 分类编码修改保护；
 * 4. 分类 URL 补全逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SignCategoryServiceTest {

    /**
     * 手势分类 Mapper Mock。
     */
    @Mock
    private SignCategoryMapper signCategoryMapper;

    /**
     * 手势资源 Mapper Mock。
     */
    @Mock
    private SignResourceMapper signResourceMapper;

    /**
     * MinIO 配置 Mock。
     */
    @Mock
    private MinioProperties minioProperties;

    /**
     * 被测试的手势分类 Service。
     */
    private SignCategoryService signCategoryService;

    /**
     * 每个测试用例执行前初始化 Service。
     */
    @BeforeEach
    void setUp() {
        signCategoryService = new SignCategoryService(
                signCategoryMapper,
                signResourceMapper,
                minioProperties
        );
    }

    /**
     * 测试：查询分类列表时，应补全 coverUrl。
     */
    @Test
    void listAll_shouldFillCoverUrl() {
        SignCategory category = buildCategory(1L, "alphabet", "字母", "sign/category/alphabet.png");

        when(signCategoryMapper.selectAll()).thenReturn(List.of(category));
        when(minioProperties.buildObjectUrl("sign/category/alphabet.png"))
                .thenReturn("http://127.0.0.1:9000/cwasa-static/sign/category/alphabet.png");

        List<SignCategory> result = signCategoryService.listAll();

        assertEquals(1, result.size());
        assertEquals("alphabet", result.get(0).getCode());
        assertEquals("http://127.0.0.1:9000/cwasa-static/sign/category/alphabet.png", result.get(0).getCoverUrl());

        verify(signCategoryMapper).selectAll();
        verify(minioProperties).buildObjectUrl("sign/category/alphabet.png");
    }

    /**
     * 测试：新增分类成功。
     */
    @Test
    void create_shouldInsertCategory_whenCodeNotExists() {
        SignCategory input = buildCategory(null, "test_category", "测试分类", "sign/category/test.png");
        SignCategory saved = buildCategory(10L, "test_category", "测试分类", "sign/category/test.png");

        when(signCategoryMapper.selectByCode("test_category")).thenReturn(null);

        doAnswer(invocation -> {
            SignCategory category = invocation.getArgument(0);
            category.setId(10L);
            return 1;
        }).when(signCategoryMapper).insert(input);

        when(signCategoryMapper.selectById(10L)).thenReturn(saved);
        when(minioProperties.buildObjectUrl("sign/category/test.png"))
                .thenReturn("http://127.0.0.1:9000/cwasa-static/sign/category/test.png");

        SignCategory result = signCategoryService.create(input);

        assertEquals(10L, result.getId());
        assertEquals("test_category", result.getCode());
        assertEquals("测试分类", result.getNameZh());
        assertEquals("http://127.0.0.1:9000/cwasa-static/sign/category/test.png", result.getCoverUrl());

        verify(signCategoryMapper).selectByCode("test_category");
        verify(signCategoryMapper).insert(input);
        verify(signCategoryMapper).selectById(10L);
    }

    /**
     * 测试：新增分类时，如果分类编码已存在，应抛出异常。
     */
    @Test
    void create_shouldThrowException_whenCodeExists() {
        SignCategory input = buildCategory(null, "alphabet", "字母", "sign/category/alphabet.png");
        SignCategory existed = buildCategory(1L, "alphabet", "字母", "sign/category/alphabet.png");

        when(signCategoryMapper.selectByCode("alphabet")).thenReturn(existed);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signCategoryService.create(input)
        );

        assertEquals("分类编码已存在：alphabet", exception.getMessage());

        verify(signCategoryMapper).selectByCode("alphabet");
        verify(signCategoryMapper, never()).insert(any());
    }

    /**
     * 测试：删除分类时，如果分类下仍有资源，应禁止删除。
     */
    @Test
    void deleteById_shouldThrowException_whenCategoryHasResources() {
        SignCategory category = buildCategory(11L, "alphabet", "字母", "sign/category/alphabet.png");

        when(signCategoryMapper.selectById(11L)).thenReturn(category);
        when(signResourceMapper.countByCategoryCode("alphabet")).thenReturn(3);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signCategoryService.deleteById(11L)
        );

        assertEquals("当前分类下仍有手势资源，不能删除：字母（alphabet）", exception.getMessage());

        verify(signCategoryMapper).selectById(11L);
        verify(signResourceMapper).countByCategoryCode("alphabet");
        verify(signCategoryMapper, never()).deleteById(anyLong());
    }

    /**
     * 测试：删除分类时，如果分类下没有资源，应允许删除。
     */
    @Test
    void deleteById_shouldDeleteCategory_whenNoResourceReferences() {
        SignCategory category = buildCategory(20L, "empty_category", "空分类", "sign/category/empty.png");

        when(signCategoryMapper.selectById(20L)).thenReturn(category);
        when(signResourceMapper.countByCategoryCode("empty_category")).thenReturn(0);
        when(signCategoryMapper.deleteById(20L)).thenReturn(1);

        signCategoryService.deleteById(20L);

        verify(signCategoryMapper).selectById(20L);
        verify(signResourceMapper).countByCategoryCode("empty_category");
        verify(signCategoryMapper).deleteById(20L);
    }

    /**
     * 测试：已有资源引用的分类，不允许修改分类编码。
     */
    @Test
    void update_shouldThrowException_whenCategoryHasResourcesAndCodeChanged() {
        SignCategory oldCategory = buildCategory(11L, "alphabet", "字母", "sign/category/alphabet.png");
        SignCategory input = buildCategory(null, "alphabet_new", "字母", "sign/category/alphabet.png");

        when(signCategoryMapper.selectById(11L)).thenReturn(oldCategory);
        when(signCategoryMapper.selectByCode("alphabet_new")).thenReturn(null);
        when(signResourceMapper.countByCategoryCode("alphabet")).thenReturn(5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signCategoryService.update(11L, input)
        );

        assertEquals("当前分类下仍有手势资源，不能修改分类编码：字母（alphabet）", exception.getMessage());

        verify(signCategoryMapper).selectById(11L);
        verify(signCategoryMapper).selectByCode("alphabet_new");
        verify(signResourceMapper).countByCategoryCode("alphabet");
        verify(signCategoryMapper, never()).updateById(any());
    }

    /**
     * 测试：已有资源引用的分类，保持 code 不变时，允许修改名称和封面。
     */
    @Test
    void update_shouldAllowUpdateNameAndCover_whenCodeNotChanged() {
        SignCategory oldCategory = buildCategory(11L, "alphabet", "字母", "sign/category/alphabet.png");
        SignCategory input = buildCategory(null, "alphabet", "字母分类", "sign/category/alphabet-new.png");
        SignCategory updated = buildCategory(11L, "alphabet", "字母分类", "sign/category/alphabet-new.png");

        when(signCategoryMapper.selectById(11L)).thenReturn(oldCategory);
        when(signCategoryMapper.selectByCode("alphabet")).thenReturn(oldCategory);
        when(signCategoryMapper.updateById(input)).thenReturn(1);
        when(signCategoryMapper.selectById(11L)).thenReturn(oldCategory, updated);
        when(minioProperties.buildObjectUrl("sign/category/alphabet-new.png"))
                .thenReturn("http://127.0.0.1:9000/cwasa-static/sign/category/alphabet-new.png");

        SignCategory result = signCategoryService.update(11L, input);

        assertEquals(11L, result.getId());
        assertEquals("alphabet", result.getCode());
        assertEquals("字母分类", result.getNameZh());
        assertEquals("http://127.0.0.1:9000/cwasa-static/sign/category/alphabet-new.png", result.getCoverUrl());

        verify(signCategoryMapper, times(2)).selectById(11L);
        verify(signCategoryMapper).selectByCode("alphabet");
        verify(signCategoryMapper).updateById(input);
        verify(signResourceMapper, never()).countByCategoryCode(anyString());
    }

    /**
     * 构造测试用分类实体。
     *
     * @param id             主键 ID
     * @param code           分类编码
     * @param nameZh         中文名称
     * @param coverObjectKey 封面对象 Key
     * @return 分类实体
     */
    private SignCategory buildCategory(Long id, String code, String nameZh, String coverObjectKey) {
        SignCategory category = new SignCategory();

        category.setId(id);
        category.setCode(code);
        category.setNameZh(nameZh);
        category.setCoverObjectKey(coverObjectKey);

        return category;
    }
}
