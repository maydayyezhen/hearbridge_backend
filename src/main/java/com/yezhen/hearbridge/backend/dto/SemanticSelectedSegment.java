package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 后处理对某个词段的选择动作。
 */
@Getter
@Setter
public class SemanticSelectedSegment {

    private Integer segmentIndex;

    private String rawLabel;

    /**
     * 删除词段时为 null。
     */
    private String selectedLabel;

    /**
     * keep / remove。
     */
    private String action;
}
