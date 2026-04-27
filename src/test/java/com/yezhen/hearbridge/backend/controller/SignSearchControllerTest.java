package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.PageResult;
import com.yezhen.hearbridge.backend.dto.SignSearchResult;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.service.SignSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 手语训练统一搜索 Controller 黑盒测试。
 *
 * 使用 MockMvc 模拟 HTTP 请求，
 * 只验证接口输入输出，不依赖真实数据库。
 */
class SignSearchControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * 手语训练统一搜索 Service Mock。
     */
    private SignSearchService signSearchService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        signSearchService = Mockito.mock(SignSearchService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignSearchController(signSearchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：GET /sign/search 空关键词返回空分页结果。
     */
    @Test
    void search_shouldReturnEmptyResult_whenKeywordIsBlank() throws Exception {
        Mockito.when(signSearchService.search(isNull(), isNull(), isNull()))
                .thenReturn(new SignSearchResult(
                        "",
                        PageResult.of(List.of(), 0, 1, 20),
                        PageResult.of(List.of(), 0, 1, 20)
                ));

        mockMvc.perform(get("/sign/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value(""))
                .andExpect(jsonPath("$.categories.records.length()").value(0))
                .andExpect(jsonPath("$.categories.total").value(0))
                .andExpect(jsonPath("$.resources.records.length()").value(0))
                .andExpect(jsonPath("$.resources.total").value(0));
    }

    /**
     * 测试：GET /sign/search 能返回匹配分类和手势资源分页结果。
     */
    @Test
    void search_shouldReturnCategoriesAndResources() throws Exception {
        SignCategory category = buildCategory(
                1L,
                "daily",
                "日常表达",
                "sign/category/daily.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/daily.png"
        );
        SignResource resource = buildResource(
                10L,
                "hello",
                "你好",
                "daily",
                "sign/sigml/hello.sigml",
                "sign/resource/hello.png",
                "http://127.0.0.1:9000/cwasa-static/sign/sigml/hello.sigml",
                "http://127.0.0.1:9000/cwasa-static/sign/resource/hello.png"
        );

        Mockito.when(signSearchService.search(eq("你"), eq(1), eq(10)))
                .thenReturn(new SignSearchResult(
                        "你",
                        PageResult.of(List.of(category), 1, 1, 10),
                        PageResult.of(List.of(resource), 1, 1, 10)
                ));

        mockMvc.perform(get("/sign/search")
                        .param("keyword", "你")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("你"))
                .andExpect(jsonPath("$.categories.records[0].id").value(1))
                .andExpect(jsonPath("$.categories.records[0].code").value("daily"))
                .andExpect(jsonPath("$.categories.records[0].nameZh").value("日常表达"))
                .andExpect(jsonPath("$.categories.total").value(1))
                .andExpect(jsonPath("$.categories.pageNo").value(1))
                .andExpect(jsonPath("$.categories.pageSize").value(10))
                .andExpect(jsonPath("$.resources.records[0].id").value(10))
                .andExpect(jsonPath("$.resources.records[0].code").value("hello"))
                .andExpect(jsonPath("$.resources.records[0].nameZh").value("你好"))
                .andExpect(jsonPath("$.resources.records[0].categoryCode").value("daily"))
                .andExpect(jsonPath("$.resources.records[0].sigmlUrl").value("http://127.0.0.1:9000/cwasa-static/sign/sigml/hello.sigml"))
                .andExpect(jsonPath("$.resources.records[0].coverUrl").value("http://127.0.0.1:9000/cwasa-static/sign/resource/hello.png"))
                .andExpect(jsonPath("$.resources.total").value(1))
                .andExpect(jsonPath("$.resources.pageNo").value(1))
                .andExpect(jsonPath("$.resources.pageSize").value(10));
    }

    /**
     * 测试：GET /sign/search 搜索服务抛出业务异常时返回 400。
     */
    @Test
    void search_shouldReturnBadRequest_whenServiceThrowsIllegalArgumentException() throws Exception {
        Mockito.when(signSearchService.search(eq("bad"), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("搜索参数异常"));

        mockMvc.perform(get("/sign/search").param("keyword", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("搜索参数异常"))
                .andExpect(jsonPath("$.path").value("/sign/search"));
    }

    /**
     * 构造分类实体。
     *
     * @param id             主键 ID
     * @param code           分类编码
     * @param nameZh         中文名称
     * @param coverObjectKey 封面对象 Key
     * @param coverUrl       封面 URL
     * @return 分类实体
     */
    private SignCategory buildCategory(
            Long id,
            String code,
            String nameZh,
            String coverObjectKey,
            String coverUrl) {
        SignCategory category = new SignCategory();

        category.setId(id);
        category.setCode(code);
        category.setNameZh(nameZh);
        category.setCoverObjectKey(coverObjectKey);
        category.setCoverUrl(coverUrl);

        return category;
    }

    /**
     * 构造手势资源实体。
     *
     * @param id             主键 ID
     * @param code           资源编码
     * @param nameZh         中文名称
     * @param categoryCode   分类编码
     * @param sigmlObjectKey SiGML 对象 Key
     * @param coverObjectKey 封面对象 Key
     * @param sigmlUrl       SiGML URL
     * @param coverUrl       封面 URL
     * @return 手势资源实体
     */
    private SignResource buildResource(
            Long id,
            String code,
            String nameZh,
            String categoryCode,
            String sigmlObjectKey,
            String coverObjectKey,
            String sigmlUrl,
            String coverUrl) {
        SignResource resource = new SignResource();

        resource.setId(id);
        resource.setCode(code);
        resource.setNameZh(nameZh);
        resource.setCategoryCode(categoryCode);
        resource.setSigmlObjectKey(sigmlObjectKey);
        resource.setCoverObjectKey(coverObjectKey);
        resource.setSigmlUrl(sigmlUrl);
        resource.setCoverUrl(coverUrl);

        return resource;
    }
}
