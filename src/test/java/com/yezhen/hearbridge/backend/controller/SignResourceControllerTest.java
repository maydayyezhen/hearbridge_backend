package com.yezhen.hearbridge.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.entity.SignResource;
import com.yezhen.hearbridge.backend.service.SignResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 手势资源 Controller 黑盒测试。
 *
 * 使用 MockMvc 模拟 HTTP 请求，
 * 验证资源管理接口的输入输出。
 */
class SignResourceControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * JSON 序列化工具。
     */
    private ObjectMapper objectMapper;

    /**
     * 手势资源 Service Mock。
     */
    private SignResourceService signResourceService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        signResourceService = Mockito.mock(SignResourceService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignResourceController(signResourceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：GET /sign/resources 查询资源列表。
     */
    @Test
    void listResources_shouldReturnResourceList() throws Exception {
        SignResource resource = buildResource(
                1L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png",
                "http://127.0.0.1:9000/cwasa-static/sign/resource/sigml/hello.sigml",
                "http://127.0.0.1:9000/cwasa-static/sign/resource/cover/hello.png"
        );

        Mockito.when(signResourceService.list(null)).thenReturn(List.of(resource));

        mockMvc.perform(get("/sign/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("hello"))
                .andExpect(jsonPath("$[0].nameZh").value("你好"))
                .andExpect(jsonPath("$[0].categoryCode").value("daily_greeting"))
                .andExpect(jsonPath("$[0].sigmlUrl").value("http://127.0.0.1:9000/cwasa-static/sign/resource/sigml/hello.sigml"))
                .andExpect(jsonPath("$[0].coverUrl").value("http://127.0.0.1:9000/cwasa-static/sign/resource/cover/hello.png"));
    }

    /**
     * 测试：GET /sign/resources?categoryCode=alphabet 按分类查询。
     */
    @Test
    void listResources_shouldQueryByCategoryCode() throws Exception {
        SignResource resource = buildResource(
                2L,
                "a",
                "字母A",
                "alphabet",
                "sign/resource/sigml/a.sigml",
                "sign/resource/cover/a.png",
                null,
                null
        );

        Mockito.when(signResourceService.list("alphabet")).thenReturn(List.of(resource));

        mockMvc.perform(get("/sign/resources").param("categoryCode", "alphabet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("a"))
                .andExpect(jsonPath("$[0].categoryCode").value("alphabet"));
    }

    /**
     * 测试：GET /sign/resources/{code} 查询资源详情。
     */
    @Test
    void getResource_shouldReturnResourceByCode() throws Exception {
        SignResource resource = buildResource(
                1L,
                "hello",
                "你好",
                "daily_greeting",
                "sign/resource/sigml/hello.sigml",
                "sign/resource/cover/hello.png",
                null,
                null
        );

        Mockito.when(signResourceService.getByCode("hello")).thenReturn(resource);

        mockMvc.perform(get("/sign/resources/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("hello"))
                .andExpect(jsonPath("$.nameZh").value("你好"));
    }

    /**
     * 测试：POST /sign/resources 新增资源。
     */
    @Test
    void createResource_shouldReturnCreatedResource() throws Exception {
        SignResource input = buildResource(
                null,
                "test_resource",
                "测试资源",
                "alphabet",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png",
                null,
                null
        );
        SignResource created = buildResource(
                20L,
                "test_resource",
                "测试资源",
                "alphabet",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png",
                null,
                null
        );

        Mockito.when(signResourceService.create(any(SignResource.class))).thenReturn(created);

        mockMvc.perform(post("/sign/resources")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.code").value("test_resource"))
                .andExpect(jsonPath("$.categoryCode").value("alphabet"));
    }

    /**
     * 测试：POST /sign/resources 所属分类不存在时返回 400。
     */
    @Test
    void createResource_shouldReturnBadRequest_whenCategoryNotExists() throws Exception {
        SignResource input = buildResource(
                null,
                "test_resource",
                "测试资源",
                "missing_category",
                "sign/resource/sigml/test.sigml",
                "sign/resource/cover/test.png",
                null,
                null
        );

        Mockito.when(signResourceService.create(any(SignResource.class)))
                .thenThrow(new IllegalArgumentException("所属分类不存在：missing_category"));

        mockMvc.perform(post("/sign/resources")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("所属分类不存在：missing_category"));
    }

    /**
     * 测试：PUT /sign/resources/{id} 更新资源。
     */
    @Test
    void updateResource_shouldReturnUpdatedResource() throws Exception {
        SignResource input = buildResource(
                null,
                "hello",
                "你好更新",
                "daily_greeting",
                "sign/resource/sigml/hello-new.sigml",
                "sign/resource/cover/hello-new.png",
                null,
                null
        );
        SignResource updated = buildResource(
                1L,
                "hello",
                "你好更新",
                "daily_greeting",
                "sign/resource/sigml/hello-new.sigml",
                "sign/resource/cover/hello-new.png",
                null,
                null
        );

        Mockito.when(signResourceService.update(eq(1L), any(SignResource.class))).thenReturn(updated);

        mockMvc.perform(put("/sign/resources/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nameZh").value("你好更新"));
    }

    /**
     * 测试：DELETE /sign/resources/{id} 删除资源。
     */
    @Test
    void deleteResource_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/sign/resources/20"))
                .andExpect(status().isOk());

        Mockito.verify(signResourceService).deleteById(20L);
    }

    /**
     * 构造资源实体。
     *
     * @param id              主键 ID
     * @param code            资源编码
     * @param nameZh          中文名称
     * @param categoryCode    分类编码
     * @param sigmlObjectKey  SiGML 对象 Key
     * @param coverObjectKey  封面对象 Key
     * @param sigmlUrl        SiGML URL
     * @param coverUrl        封面 URL
     * @return 资源实体
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
