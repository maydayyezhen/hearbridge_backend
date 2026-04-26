package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.config.MinioProperties;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
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
 * 手势资源 Service 白盒测试。
 *
 * 测试重点：
 * 1. 资源编码唯一性；
 * 2. 所属分类存在性；
 * 3. 资源 CRUD 业务规则；
 * 4. 资源 URL 补全逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SignResourceServiceTest {

    /**
     * 手势资源 Mapper Mock。
     */
    @Mock
    private SignResourceMapper signResourceMapper;

    /**
     * 手势分类 Mapper Mock。
     */
    @Mock
    private SignCategoryMapper signCategoryMapper;

    /**
     * MinIO 配置 Mock。
     */
    @Mock
    private MinioProperties minioProperties;

    /**
     * 被测试的手势资源 Service。
     */
    private SignResourceService signResourceService;

    /**
     * 每个测试用例执行前初始化 Service。
     */
    @BeforeEach
    void setUp() {
        signResourceService = new SignResourceService(
                signResourceMapper,
                signCategoryMapper,
                minioProperties
        );
    }

    /**
     * 测试：查询资源列表时，应补全 coverUrl 和 sigmlUrl。
     */
    @Test
    void list_shouldFillUrls() {
        SignResource resource = buildResource(
                1L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png"
        );

        when(signResourceMapper.selectAll()).thenReturn(List.of(resource));
        when(minioProperties.buildObjectUrl("sign/resource/sigml/hello.sigml"))
                .thenReturn("http://127.0.0.1:9000/cwasa-static/sign/resource/sigml/hello.sigml");
        when(minioProperties.buildObjectUrl("sign/resource/cover/hello.png"))
                .thenReturn("http://127.0.0.1:9000/cwasa-static/sign/resource/cover/hello.png");

        List<SignResource> result = signResourceService.list(null);

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).getCode());
        assertEquals("http://127.0.0.1:9000/cwasa-static/sign/resource/sigml/hello.sigml", result.get(0).getSigmlUrl());
        assertEquals("http://127.0.0.1:9000/cwasa-static/sign/resource/cover/hello.png", result.get(0).getCoverUrl());

        verify(signResourceMapper).selectAll();
        verify(minioProperties).buildObjectUrl("sign/resource/sigml/hello.sigml");
        verify(minioProperties).buildObjectUrl("sign/resource/cover/hello.png");
    }

    /**
     * 测试：按分类编码查询资源列表。
     */
    @Test
    void list_shouldQueryByCategoryCode_whenCategoryCodeExists() {
        SignResource resource = buildResource(
                1L,
                "a",
                "字母A",
                "alphabet",
                "sign/resource/sigml/a.sigml",
                "sign/resource/cover/a.png"
        );

        when(signResourceMapper.selectByCategoryCode("alphabet")).thenReturn(List.of(resource));

        List<SignResource> result = signResourceService.list("alphabet");

        assertEquals(1, result.size());
        assertEquals("alphabet", result.get(0).getCategoryCode());

        verify(signResourceMapper).selectByCategoryCode("alphabet");
        verify(signResourceMapper, never()).selectAll();
    }

    /**
     * 测试：新增资源成功。
     */
    @Test
    void create_shouldInsertResource_whenInputValid() {
        SignResource input = buildResource(
                null,
                "test_resource",
                "测试资源",
                "alphabet",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png"
        );
        SignResource saved = buildResource(
                10L,
                "test_resource",
                "测试资源",
                "alphabet",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png"
        );
        SignCategory category = buildCategory(1L, "alphabet", "字母");

        when(signResourceMapper.selectByCode("test_resource")).thenReturn(null);
        when(signCategoryMapper.selectByCode("alphabet")).thenReturn(category);

        doAnswer(invocation -> {
            SignResource resource = invocation.getArgument(0);
            resource.setId(10L);
            return 1;
        }).when(signResourceMapper).insert(input);

        when(signResourceMapper.selectById(10L)).thenReturn(saved);

        SignResource result = signResourceService.create(input);

        assertEquals(10L, result.getId());
        assertEquals("test_resource", result.getCode());
        assertEquals("alphabet", result.getCategoryCode());

        verify(signResourceMapper).selectByCode("test_resource");
        verify(signCategoryMapper).selectByCode("alphabet");
        verify(signResourceMapper).insert(input);
        verify(signResourceMapper).selectById(10L);
    }

    /**
     * 测试：新增资源时，如果资源编码重复，应抛出异常。
     */
    @Test
    void create_shouldThrowException_whenResourceCodeExists() {
        SignResource input = buildResource(
                null,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png"
        );
        SignResource existed = buildResource(
                1L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png"
        );

        when(signResourceMapper.selectByCode("hello")).thenReturn(existed);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signResourceService.create(input)
        );

        assertEquals("资源编码已存在：hello", exception.getMessage());

        verify(signResourceMapper).selectByCode("hello");
        verify(signCategoryMapper, never()).selectByCode(anyString());
        verify(signResourceMapper, never()).insert(any());
    }

    /**
     * 测试：新增资源时，如果所属分类不存在，应抛出异常。
     */
    @Test
    void create_shouldThrowException_whenCategoryNotExists() {
        SignResource input = buildResource(
                null,
                "test_resource",
                "测试资源",
                "missing_category",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png"
        );

        when(signResourceMapper.selectByCode("test_resource")).thenReturn(null);
        when(signCategoryMapper.selectByCode("missing_category")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signResourceService.create(input)
        );

        assertEquals("所属分类不存在：missing_category", exception.getMessage());

        verify(signResourceMapper).selectByCode("test_resource");
        verify(signCategoryMapper).selectByCode("missing_category");
        verify(signResourceMapper, never()).insert(any());
    }

    /**
     * 测试：更新资源成功。
     */
    @Test
    void update_shouldUpdateResource_whenInputValid() {
        SignResource oldResource = buildResource(
                10L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png"
        );
        SignResource input = buildResource(
                null,
                "hello",
                "你好更新",
                "daily_greeting",
                "sign/resource/sigml/hello-new.sigml",
                "sign/resource/cover/hello-new.png"
        );
        SignResource updated = buildResource(
                10L,
                "hello",
                "你好更新",
                "daily_greeting",
                "sign/resource/sigml/hello-new.sigml",
                "sign/resource/cover/hello-new.png"
        );
        SignCategory category = buildCategory(1L, "daily_greeting", "日常问候");

        when(signResourceMapper.selectById(10L)).thenReturn(oldResource, updated);
        when(signResourceMapper.selectByCode("hello")).thenReturn(oldResource);
        when(signCategoryMapper.selectByCode("daily_greeting")).thenReturn(category);
        when(signResourceMapper.updateById(input)).thenReturn(1);

        SignResource result = signResourceService.update(10L, input);

        assertEquals(10L, result.getId());
        assertEquals("hello", result.getCode());
        assertEquals("你好更新", result.getNameZh());

        verify(signResourceMapper, times(2)).selectById(10L);
        verify(signResourceMapper).selectByCode("hello");
        verify(signCategoryMapper).selectByCode("daily_greeting");
        verify(signResourceMapper).updateById(input);
    }

    /**
     * 测试：删除资源成功。
     */
    @Test
    void deleteById_shouldDeleteResource_whenResourceExists() {
        SignResource oldResource = buildResource(
                10L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png"
        );

        when(signResourceMapper.selectById(10L)).thenReturn(oldResource);
        when(signResourceMapper.deleteById(10L)).thenReturn(1);

        signResourceService.deleteById(10L);

        verify(signResourceMapper).selectById(10L);
        verify(signResourceMapper).deleteById(10L);
    }

    /**
     * 测试：删除不存在的资源时，应抛出异常。
     */
    @Test
    void deleteById_shouldThrowException_whenResourceNotExists() {
        when(signResourceMapper.selectById(404L)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signResourceService.deleteById(404L)
        );

        assertEquals("资源不存在，ID：404", exception.getMessage());

        verify(signResourceMapper).selectById(404L);
        verify(signResourceMapper, never()).deleteById(anyLong());
    }

    /**
     * 构造测试用手势资源实体。
     *
     * @param id              主键 ID
     * @param code            资源编码
     * @param nameZh          中文名称
     * @param categoryCode    分类编码
     * @param sigmlObjectKey  SiGML 对象 Key
     * @param coverObjectKey  封面对象 Key
     * @return 手势资源实体
     */
    private SignResource buildResource(
            Long id,
            String code,
            String nameZh,
            String categoryCode,
            String sigmlObjectKey,
            String coverObjectKey) {
        SignResource resource = new SignResource();

        resource.setId(id);
        resource.setCode(code);
        resource.setNameZh(nameZh);
        resource.setCategoryCode(categoryCode);
        resource.setSigmlObjectKey(sigmlObjectKey);
        resource.setCoverObjectKey(coverObjectKey);

        return resource;
    }

    /**
     * 构造测试用分类实体。
     *
     * @param id     分类 ID
     * @param code   分类编码
     * @param nameZh 中文名称
     * @return 分类实体
     */
    private SignCategory buildCategory(Long id, String code, String nameZh) {
        SignCategory category = new SignCategory();

        category.setId(id);
        category.setCode(code);
        category.setNameZh(nameZh);

        return category;
    }
}
