package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 句子视频识别词段 TopK 候选项。
 */
@Getter
@Setter
public class SentenceSegmentTopKItem {

    /**
     * 英文 gloss 标签。
     */
    private String label;

    /**
     * 中文展示标签。
     */
    private String labelZh;

    /**
     * 该候选词在词段覆盖滑窗中的平均概率。
     */
    private Double avgProb;

    /**
     * 该候选词在词段覆盖滑窗中的最大概率。
     */
    private Double maxProb;

    /**
     * 该候选词在词段覆盖滑窗 TopK 中出现的次数。
     */
    private Integer hitCount;
}
