package com.yezhen.hearbridge.backend.service;

import com.yezhen.hearbridge.backend.dto.SentenceVideoRecognizeResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 手语句子视频识别业务服务。
 *
 * 当前职责：
 * 1. 接收 Controller 上传的视频文件；
 * 2. 调用 Python 视觉识别服务；
 * 3. 返回原始识别结果。
 *
 * 注意：
 * DeepSeek 语义增强不在本服务中处理，后续单独实现。
 */
@Service
public class SignVideoRecognitionService {

    /**
     * Python 手势识别服务客户端。
     */
    private final PythonGestureServiceClient pythonGestureServiceClient;

    /**
     * 构造注入。
     *
     * @param pythonGestureServiceClient Python 手势识别服务客户端
     */
    public SignVideoRecognitionService(PythonGestureServiceClient pythonGestureServiceClient) {
        this.pythonGestureServiceClient = pythonGestureServiceClient;
    }

    /**
     * 识别上传的句子视频。
     *
     * @param file 上传的视频文件
     * @return 句子视频识别结果
     */
    public SentenceVideoRecognizeResult recognizeSentenceVideo(MultipartFile file) {
        return pythonGestureServiceClient.recognizeSentenceVideo(file);
    }
}
