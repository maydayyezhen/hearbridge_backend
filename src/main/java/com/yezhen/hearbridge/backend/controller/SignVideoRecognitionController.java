package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.SentenceVideoRecognizeResult;
import com.yezhen.hearbridge.backend.service.SignVideoRecognitionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 手语句子视频识别 Controller。
 *
 * 当前提供：
 * 1. 上传句子视频；
 * 2. 调用 Python 识别服务；
 * 3. 返回原始识别结果。
 */
@RestController
@RequestMapping("/app/sign-video")
public class SignVideoRecognitionController {

    /**
     * 手语句子视频识别业务服务。
     */
    private final SignVideoRecognitionService signVideoRecognitionService;

    /**
     * 构造注入。
     *
     * @param signVideoRecognitionService 手语句子视频识别业务服务
     */
    public SignVideoRecognitionController(SignVideoRecognitionService signVideoRecognitionService) {
        this.signVideoRecognitionService = signVideoRecognitionService;
    }

    /**
     * 上传句子视频并进行识别。
     *
     * 请求类型：
     * multipart/form-data
     *
     * 参数：
     * file：视频文件，支持 mp4/mov/avi/mkv，具体限制由 Python 服务校验。
     *
     * @param file 上传视频
     * @return 句子视频识别结果
     */
    @PostMapping(
            value = "/recognize",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public SentenceVideoRecognizeResult recognize(
            @RequestParam("file") MultipartFile file) {
        return signVideoRecognitionService.recognizeSentenceVideo(file);
    }
}
