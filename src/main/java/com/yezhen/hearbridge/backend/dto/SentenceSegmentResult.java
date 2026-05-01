package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 句子视频识别出的单个词段。
 */
@Getter
@Setter
public class SentenceSegmentResult {

    /**
     * 词段序号，从 1 开始。
     */
    private Integer segmentIndex;

    /**
     * 起始帧。
     */
    private Integer startFrame;

    /**
     * 结束帧。
     */
    private Integer endFrame;

    /**
     * 当前词段原始识别英文 gloss。
     */
    private String rawLabel;

    /**
     * 当前词段中文展示。
     */
    private String rawLabelZh;

    /**
     * 当前词段平均置信度。
     */
    private Double avgConfidence;

    /**
     * 当前词段最大置信度。
     */
    private Double maxConfidence;

    /**
     * 当前词段 TopK 候选。
     */
    private List<SentenceSegmentTopKItem> topK;
}
