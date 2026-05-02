package com.yezhen.hearbridge.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 语义修正删除的词段。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticRemovedSegment {

    private Integer segmentIndex;

    private String rawLabel;

    private String rawLabelZh;

    private String reason;

    private String reasonZh;
}
