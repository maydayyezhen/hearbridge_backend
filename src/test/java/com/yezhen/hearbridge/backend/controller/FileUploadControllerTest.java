package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.FileUploadResult;
import com.yezhen.hearbridge.backend.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件上传 Controller 黑盒测试。
 *
 * 注意：
 * 这里不连接真实 MinIO。
 * 只通过 Mock MinioStorageService 验证上传接口输入输出。
 */
class FileUploadControllerTest {

    /**
     * MockMvc 测试客户端。
     */
    private MockMvc mockMvc;

    /**
     * MinIO 存储服务 Mock。
     */
    private MinioStorageService minioStorageService;

    /**
     * 每个测试用例执行前初始化 MockMvc。
     */
    @BeforeEach
    void setUp() {
        minioStorageService = Mockito.mock(MinioStorageService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FileUploadController(minioStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 测试：POST /files/upload 上传分类封面图片成功。
     */
    @Test
    void upload_shouldReturnUploadResult_whenImageValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "fake image content".getBytes()
        );

        FileUploadResult result = new FileUploadResult(
                "cwasa-static",
                "sign/category/20260426-test.png",
                "http://127.0.0.1:9000/cwasa-static/sign/category/20260426-test.png",
                "test.png",
                "image/png",
                18L
        );

        Mockito.when(minioStorageService.uploadByBizType(any(), eq("sign-category-cover")))
                .thenReturn(result);

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("bizType", "sign-category-cover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("cwasa-static"))
                .andExpect(jsonPath("$.objectKey").value("sign/category/20260426-test.png"))
                .andExpect(jsonPath("$.url").value("http://127.0.0.1:9000/cwasa-static/sign/category/20260426-test.png"))
                .andExpect(jsonPath("$.originalFileName").value("test.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(18));
    }

    /**
     * 测试：POST /files/upload 不支持的业务类型返回 400。
     */
    @Test
    void upload_shouldReturnBadRequest_whenBizTypeUnsupported() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "fake image content".getBytes()
        );

        Mockito.when(minioStorageService.uploadByBizType(any(), eq("unknown-biz-type")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的上传业务类型：unknown-biz-type"));

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("bizType", "unknown-biz-type"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("不支持的上传业务类型：unknown-biz-type"));
    }

    /**
     * 测试：POST /files/upload 缺少 file 时返回 400。
     *
     * 注意：
     * 缺少 multipart part 时会由 Spring MVC 在进入 Controller 前拦截。
     */
    @Test
    void upload_shouldReturnBadRequest_whenFileMissing() throws Exception {
        mockMvc.perform(multipart("/files/upload")
                        .param("bizType", "sign-category-cover"))
                .andExpect(status().isBadRequest());
    }
}
