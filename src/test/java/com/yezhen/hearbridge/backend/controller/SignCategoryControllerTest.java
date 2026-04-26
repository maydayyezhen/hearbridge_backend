package com.yezhen.hearbridge.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.entity.SignCategory;
import com.yezhen.hearbridge.backend.service.SignCategoryService;
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
 * 手势分类 Controller 黑盒测试。
 *
 * 使用 MockMvc 模拟 HTTP 请求，
 * 只验证接口输入输出，不依赖真实数据库。
 */
class SignCategoryControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * JSON 序列化工具。
     */
    private ObjectMapper objectMapper;

    /**
     * 手势分类 Service Mock。
     */
    private SignCategoryService signCategoryService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        signCategoryService = Mockito.mock(SignCategoryService.class);
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SignCategoryController(signCategoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：GET /sign/categories 查询分类列表。
     */
    @Test
    void listCategories_shouldReturnCategoryList() throws Exception {
        SignCategory category = buildCategory(
                1L,
                "alphabet",
                "字母",
                "sign/category/alphabet.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/alphabet.png"
        );

        Mockito.when(signCategoryService.listAll()).thenReturn(List.of(category));

        mockMvc.perform(get("/sign/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("alphabet"))
                .andExpect(jsonPath("$[0].nameZh").value("字母"))
                .andExpect(jsonPath("$[0].coverObjectKey").value("sign/category/alphabet.png"))
                .andExpect(jsonPath("$[0].coverUrl").value("http://127.0.0.1:9000/cwasa-static/sign/category/alphabet.png"));
    }

    /**
     * 测试：GET /sign/categories/{code} 查询分类详情。
     */
    @Test
    void getCategory_shouldReturnCategoryByCode() throws Exception {
        SignCategory category = buildCategory(
                1L,
                "alphabet",
                "字母",
                "sign/category/alphabet.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/alphabet.png"
        );

        Mockito.when(signCategoryService.getByCode("alphabet")).thenReturn(category);

        mockMvc.perform(get("/sign/categories/alphabet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("alphabet"))
                .andExpect(jsonPath("$.nameZh").value("字母"));
    }

    /**
     * 测试：POST /sign/categories 新增分类。
     */
    @Test
    void createCategory_shouldReturnCreatedCategory() throws Exception {
        SignCategory input = buildCategory(null, "test_category", "测试分类", "sign/category/test.png", null);
        SignCategory created = buildCategory(
                10L,
                "test_category",
                "测试分类",
                "sign/category/test.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/test.png"
        );

        Mockito.when(signCategoryService.create(any(SignCategory.class))).thenReturn(created);

        mockMvc.perform(post("/sign/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.code").value("test_category"))
                .andExpect(jsonPath("$.nameZh").value("测试分类"));
    }

    /**
     * 测试：POST /sign/categories 分类编码重复时返回 400。
     */
    @Test
    void createCategory_shouldReturnBadRequest_whenCodeExists() throws Exception {
        SignCategory input = buildCategory(null, "alphabet", "字母", "sign/category/alphabet.png", null);

        Mockito.when(signCategoryService.create(any(SignCategory.class)))
                .thenThrow(new IllegalArgumentException("分类编码已存在：alphabet"));

        mockMvc.perform(post("/sign/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("分类编码已存在：alphabet"))
                .andExpect(jsonPath("$.path").value("/sign/categories"));
    }

    /**
     * 测试：PUT /sign/categories/{id} 更新分类。
     */
    @Test
    void updateCategory_shouldReturnUpdatedCategory() throws Exception {
        SignCategory input = buildCategory(null, "alphabet", "字母分类", "sign/category/alphabet-new.png", null);
        SignCategory updated = buildCategory(
                1L,
                "alphabet",
                "字母分类",
                "sign/category/alphabet-new.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/alphabet-new.png"
        );

        Mockito.when(signCategoryService.update(eq(1L), any(SignCategory.class))).thenReturn(updated);

        mockMvc.perform(put("/sign/categories/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("alphabet"))
                .andExpect(jsonPath("$.nameZh").value("字母分类"));
    }

    /**
     * 测试：DELETE /sign/categories/{id} 删除分类。
     */
    @Test
    void deleteCategory_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/sign/categories/20"))
                .andExpect(status().isOk());

        Mockito.verify(signCategoryService).deleteById(20L);
    }

    /**
     * 测试：DELETE /sign/categories/{id} 删除被资源引用的分类时返回 400。
     */
    @Test
    void deleteCategory_shouldReturnBadRequest_whenCategoryHasResources() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("当前分类下仍有手势资源，不能删除：字母（alphabet）"))
                .when(signCategoryService)
                .deleteById(11L);

        mockMvc.perform(delete("/sign/categories/11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("当前分类下仍有手势资源，不能删除：字母（alphabet）"));
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
}
