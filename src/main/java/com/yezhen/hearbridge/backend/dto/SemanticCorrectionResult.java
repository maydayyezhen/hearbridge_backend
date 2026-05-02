package com.yezhen.hearbridge.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * AI 语义修正结果。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticCorrectionResult {

    private List<String> rawSequence;

    private String rawTextZh;

    private List<String> correctedSequence;

    private String correctedTextZh;

    private Boolean correctionApplied;

    private List<SemanticSelectedSegment> selectedSegments;

    private List<SemanticRemovedSegment> removedSegments;

    private String reason;

    private String reasonZh;

    /**
     * DeepSeek 调用失败、JSON 解析失败或校验失败时为 true。
     */
    private Boolean fallback;
}
