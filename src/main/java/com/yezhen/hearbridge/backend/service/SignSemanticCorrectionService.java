package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionRequest;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionResult;
import com.yezhen.hearbridge.backend.dto.SemanticRemovedSegment;
import com.yezhen.hearbridge.backend.dto.SemanticSelectedSegment;
import com.yezhen.hearbridge.backend.dto.SentenceSegmentResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手语句子识别 AI 语义修正服务。
 */
@Service
public class SignSemanticCorrectionService {

    private static final String FALLBACK_REASON = "fallback to raw sequence";

    private static final String FALLBACK_REASON_ZH = "AI 语义修正暂不可用，已保留原始识别结果。";

    private final DeepSeekSemanticClient deepSeekSemanticClient;

    private final ObjectMapper objectMapper;

    public SignSemanticCorrectionService(
            DeepSeekSemanticClient deepSeekSemanticClient,
            ObjectMapper objectMapper) {
        this.deepSeekSemanticClient = deepSeekSemanticClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 对识别结果做 deletion-only 语义修正。
     *
     * @param request 原始识别结果
     * @return 语义修正结果；DeepSeek 不可用或输出非法时返回 fallback
     */
    public SemanticCorrectionResult correct(SemanticCorrectionRequest request) {
        SemanticCorrectionRequest safeRequest = request == null
                ? new SemanticCorrectionRequest()
                : request;

        List<String> rawLabels = resolveRawLabels(safeRequest);
        List<String> rawSequence = resolveRawSequence(safeRequest, rawLabels);
        String rawTextZh = resolveRawTextZh(safeRequest, rawSequence);

        if (rawLabels.isEmpty()) {
            return fallback(rawSequence, rawTextZh);
        }

        try {
            JsonNode correctionJson = deepSeekSemanticClient.correct(safeRequest);
            SemanticCorrectionResult aiResult = objectMapper.treeToValue(
                    correctionJson,
                    SemanticCorrectionResult.class
            );

            if (aiResult == null || aiResult.getCorrectedSequence() == null) {
                throw new IllegalStateException("DeepSeek 返回缺少 correctedSequence");
            }

            List<String> correctedSequence = normalizeRequiredLabels(aiResult.getCorrectedSequence());
            if (!isSubsequence(correctedSequence, rawLabels)) {
                throw new IllegalStateException("DeepSeek correctedSequence 不是 rawLabel 子序列");
            }

            SemanticCorrectionResult result = new SemanticCorrectionResult();
            result.setRawSequence(rawSequence);
            result.setRawTextZh(rawTextZh);
            result.setCorrectedSequence(correctedSequence);
            result.setCorrectedTextZh(resolveCorrectedTextZh(
                    safeRequest,
                    correctedSequence,
                    aiResult.getCorrectedTextZh()
            ));
            result.setCorrectionApplied(!correctedSequence.equals(rawLabels));
            result.setSelectedSegments(buildSelectedSegments(safeRequest, rawLabels, correctedSequence));
            result.setRemovedSegments(buildRemovedSegments(
                    safeRequest,
                    rawLabels,
                    correctedSequence,
                    aiResult.getRemovedSegments()
            ));
            result.setReason(StringUtils.hasText(aiResult.getReason())
                    ? aiResult.getReason().trim()
                    : "DeepSeek semantic correction completed");
            result.setReasonZh(StringUtils.hasText(aiResult.getReasonZh())
                    ? aiResult.getReasonZh().trim()
                    : "AI 语义修正已完成。");
            result.setFallback(false);

            return result;
        } catch (Exception exception) {
            return fallback(rawSequence, rawTextZh);
        }
    }

    private SemanticCorrectionResult fallback(List<String> rawSequence, String rawTextZh) {
        SemanticCorrectionResult result = new SemanticCorrectionResult();

        result.setRawSequence(rawSequence);
        result.setRawTextZh(rawTextZh);
        result.setCorrectedSequence(rawSequence);
        result.setCorrectedTextZh(rawTextZh);
        result.setCorrectionApplied(false);
        result.setSelectedSegments(Collections.emptyList());
        result.setRemovedSegments(Collections.emptyList());
        result.setReason(FALLBACK_REASON);
        result.setReasonZh(FALLBACK_REASON_ZH);
        result.setFallback(true);

        return result;
    }

    private List<String> resolveRawLabels(SemanticCorrectionRequest request) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && !segments.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (SentenceSegmentResult segment : segments) {
                if (segment != null && StringUtils.hasText(segment.getRawLabel())) {
                    labels.add(segment.getRawLabel().trim());
                }
            }
            if (!labels.isEmpty()) {
                return labels;
            }
        }

        return normalizeOptionalLabels(request.getRawSequence());
    }

    private List<String> resolveRawSequence(
            SemanticCorrectionRequest request,
            List<String> rawLabels) {
        List<String> rawSequence = normalizeOptionalLabels(request.getRawSequence());
        return rawSequence.isEmpty() ? rawLabels : rawSequence;
    }

    private String resolveRawTextZh(
            SemanticCorrectionRequest request,
            List<String> rawSequence) {
        if (StringUtils.hasText(request.getRawTextZh())) {
            return request.getRawTextZh().trim();
        }

        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && !segments.isEmpty()) {
            List<String> display = new ArrayList<>();
            for (SentenceSegmentResult segment : segments) {
                if (segment == null) {
                    continue;
                }

                if (StringUtils.hasText(segment.getRawLabelZh())) {
                    display.add(segment.getRawLabelZh().trim());
                } else if (StringUtils.hasText(segment.getRawLabel())) {
                    display.add(segment.getRawLabel().trim());
                }
            }
            if (!display.isEmpty()) {
                return String.join(" ", display);
            }
        }

        return String.join(" ", rawSequence);
    }

    private String resolveCorrectedTextZh(
            SemanticCorrectionRequest request,
            List<String> correctedSequence,
            String aiCorrectedTextZh) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && !segments.isEmpty()) {
            List<String> display = new ArrayList<>();
            int segmentCursor = 0;

            for (String label : correctedSequence) {
                while (segmentCursor < segments.size()) {
                    SentenceSegmentResult segment = segments.get(segmentCursor);
                    segmentCursor++;

                    if (segment != null && label.equals(segment.getRawLabel())) {
                        if (StringUtils.hasText(segment.getRawLabelZh())) {
                            display.add(segment.getRawLabelZh().trim());
                        } else {
                            display.add(label);
                        }
                        break;
                    }
                }
            }

            if (display.size() == correctedSequence.size()) {
                return String.join(" ", display);
            }
        }

        if (StringUtils.hasText(aiCorrectedTextZh)) {
            return aiCorrectedTextZh.trim();
        }

        return String.join(" ", correctedSequence);
    }

    private List<SemanticSelectedSegment> buildSelectedSegments(
            SemanticCorrectionRequest request,
            List<String> rawLabels,
            List<String> correctedSequence) {
        List<SemanticSelectedSegment> selectedSegments = new ArrayList<>();
        int correctedCursor = 0;

        for (int i = 0; i < rawLabels.size(); i++) {
            String rawLabel = rawLabels.get(i);
            boolean keep = correctedCursor < correctedSequence.size()
                    && rawLabel.equals(correctedSequence.get(correctedCursor));
            if (keep) {
                correctedCursor++;
            }

            SemanticSelectedSegment selectedSegment = new SemanticSelectedSegment();
            selectedSegment.setSegmentIndex(resolveSegmentIndex(request, i));
            selectedSegment.setRawLabel(rawLabel);
            selectedSegment.setSelectedLabel(keep ? rawLabel : null);
            selectedSegment.setAction(keep ? "keep" : "remove");
            selectedSegments.add(selectedSegment);
        }

        return selectedSegments;
    }

    private List<SemanticRemovedSegment> buildRemovedSegments(
            SemanticCorrectionRequest request,
            List<String> rawLabels,
            List<String> correctedSequence,
            List<SemanticRemovedSegment> aiRemovedSegments) {
        Map<Integer, SemanticRemovedSegment> aiRemovedByIndex = new HashMap<>();
        if (aiRemovedSegments != null) {
            for (SemanticRemovedSegment removedSegment : aiRemovedSegments) {
                if (removedSegment != null && removedSegment.getSegmentIndex() != null) {
                    aiRemovedByIndex.put(removedSegment.getSegmentIndex(), removedSegment);
                }
            }
        }

        List<SemanticRemovedSegment> removedSegments = new ArrayList<>();
        int correctedCursor = 0;

        for (int i = 0; i < rawLabels.size(); i++) {
            String rawLabel = rawLabels.get(i);
            boolean keep = correctedCursor < correctedSequence.size()
                    && rawLabel.equals(correctedSequence.get(correctedCursor));
            if (keep) {
                correctedCursor++;
                continue;
            }

            Integer segmentIndex = resolveSegmentIndex(request, i);
            SemanticRemovedSegment aiRemoved = aiRemovedByIndex.get(segmentIndex);

            SemanticRemovedSegment removedSegment = new SemanticRemovedSegment();
            removedSegment.setSegmentIndex(segmentIndex);
            removedSegment.setRawLabel(rawLabel);
            removedSegment.setRawLabelZh(resolveRawLabelZh(request, i, rawLabel));
            removedSegment.setReason(aiRemoved != null && StringUtils.hasText(aiRemoved.getReason())
                    ? aiRemoved.getReason().trim()
                    : "extra insertion or transition noise");
            removedSegment.setReasonZh(aiRemoved != null && StringUtils.hasText(aiRemoved.getReasonZh())
                    ? aiRemoved.getReasonZh().trim()
                    : "该词段可能是多余插入词或过渡噪声。");
            removedSegments.add(removedSegment);
        }

        return removedSegments;
    }

    private boolean isSubsequence(List<String> correctedSequence, List<String> rawLabels) {
        if (correctedSequence.size() > rawLabels.size()) {
            return false;
        }

        int rawCursor = 0;
        for (String correctedLabel : correctedSequence) {
            boolean found = false;
            while (rawCursor < rawLabels.size()) {
                if (correctedLabel.equals(rawLabels.get(rawCursor))) {
                    rawCursor++;
                    found = true;
                    break;
                }
                rawCursor++;
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    private List<String> normalizeOptionalLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalized = new ArrayList<>();
        for (String label : labels) {
            if (StringUtils.hasText(label)) {
                normalized.add(label.trim());
            }
        }
        return normalized;
    }

    private List<String> normalizeRequiredLabels(List<String> labels) {
        List<String> normalized = new ArrayList<>();
        for (String label : labels) {
            if (!StringUtils.hasText(label)) {
                throw new IllegalStateException("DeepSeek correctedSequence 包含空标签");
            }
            normalized.add(label.trim());
        }
        return normalized;
    }

    private Integer resolveSegmentIndex(SemanticCorrectionRequest request, int rawPosition) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && rawPosition < segments.size()) {
            SentenceSegmentResult segment = segments.get(rawPosition);
            if (segment != null && segment.getSegmentIndex() != null) {
                return segment.getSegmentIndex();
            }
        }

        return rawPosition + 1;
    }

    private String resolveRawLabelZh(
            SemanticCorrectionRequest request,
            int rawPosition,
            String rawLabel) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && rawPosition < segments.size()) {
            SentenceSegmentResult segment = segments.get(rawPosition);
            if (segment != null && StringUtils.hasText(segment.getRawLabelZh())) {
                return segment.getRawLabelZh().trim();
            }
        }

        return rawLabel;
    }
}
