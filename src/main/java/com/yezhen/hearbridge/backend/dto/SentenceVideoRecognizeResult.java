package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 句子视频识别结果。
 */
@Getter
@Setter
public class SentenceVideoRecognizeResult {

    /**
     * Python 服务返回状态，例如 recognized / empty。
     */
    private String status;

    /**
     * 识别模式，当前通常为 fast。
     */
    private String mode;

    /**
     * 原始英文 gloss 识别序列。
     */
    private List<String> rawSequence;

    /**
     * 原始中文展示序列。
     */
    private List<String> rawDisplayZh;

    /**
     * 原始中文展示文本。
     */
    private String rawTextZh;

    /**
     * 词段 TopK 识别详情。
     */
    private List<SentenceSegmentResult> segmentTopK;

    /**
     * Python 侧识别耗时，单位毫秒。
     */
    private Integer elapsedMs;
}
