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

    /**
     * DeepSeek 选择的程序候选序列 ID。
     */
    private String selectedCandidateId;

    /**
     * 与 selectedCandidateId 对应的候选 gloss 序列。
     */
    private List<String> correctedGlossSequence;

    private List<String> correctedSequence;

    private String correctedTextZh;

    /**
     * 忠实自然化英文句子或片段。
     */
    private String englishSentence;

    /**
     * 忠实自然化中文句子或片段。
     */
    private String chineseTranslation;

    /**
     * high / medium / low。
     */
    private String translationConfidence;

    /**
     * 语义是否不完整。
     */
    private Boolean isIncomplete;

    /**
     * 自然化翻译说明。
     */
    private String translationNote;

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
