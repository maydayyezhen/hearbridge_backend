package com.yezhen.hearbridge.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * AI 语义修正请求。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticCorrectionRequest {

    /**
     * 原始英文 gloss 识别序列。
     */
    private List<String> rawSequence;

    /**
     * 原始中文展示文本。
     */
    private String rawTextZh;

    /**
     * 词段 TopK 识别详情。
     */
    private List<SentenceSegmentResult> segmentTopK;
}
