package com.yezhen.hearbridge.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 语义修正删除的词段。
 */
@Getter
@Setter
public class SemanticRemovedSegment {

    private Integer segmentIndex;

    private String rawLabel;

    private String rawLabelZh;

    private String reason;

    private String reasonZh;
}
