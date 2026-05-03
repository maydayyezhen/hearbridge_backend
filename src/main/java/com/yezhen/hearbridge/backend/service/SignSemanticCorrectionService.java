package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionRequest;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionResult;
import com.yezhen.hearbridge.backend.dto.SemanticRemovedSegment;
import com.yezhen.hearbridge.backend.dto.SemanticSelectedSegment;
import com.yezhen.hearbridge.backend.dto.SentenceSegmentResult;
import com.yezhen.hearbridge.backend.dto.SentenceSegmentTopKItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SignSemanticCorrectionService.class);

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
            if (!rawLabels.isEmpty() && correctedSequence.isEmpty()) {
                throw new IllegalStateException("DeepSeek 不允许删除全部词段");
            }
            List<SegmentChoice> segmentChoices = alignSegmentChoices(
                    safeRequest,
                    rawLabels,
                    correctedSequence
            );
            if (segmentChoices == null) {
                throw new IllegalStateException(
                        "DeepSeek correctedSequence must be selectable from ordered rawLabel/topK segments"
                );
            }

            SemanticCorrectionResult result = new SemanticCorrectionResult();
            result.setRawSequence(rawSequence);
            result.setRawTextZh(rawTextZh);
            result.setCorrectedSequence(correctedSequence);
            result.setCorrectedTextZh(resolveCorrectedTextZh(
                    safeRequest,
                    segmentChoices,
                    aiResult.getCorrectedTextZh()
            ));
            result.setCorrectionApplied(!correctedSequence.equals(rawLabels));
            result.setSelectedSegments(buildSelectedSegments(safeRequest, rawLabels, segmentChoices));
            result.setRemovedSegments(buildRemovedSegments(
                    safeRequest,
                    rawLabels,
                    segmentChoices,
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
            log.warn("DeepSeek semantic correction fallback: {}", exception.getMessage());
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
            List<SegmentChoice> segmentChoices,
            String aiCorrectedTextZh) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && !segments.isEmpty() && segmentChoices != null && !segmentChoices.isEmpty()) {
            List<String> display = new ArrayList<>();

            for (SegmentChoice choice : segmentChoices) {
                if (!choice.isSelected()) {
                    continue;
                }
                display.add(resolveSelectedLabelZh(request, choice.getRawPosition(), choice.getSelectedLabel()));
            }

            if (!display.isEmpty()) {
                return String.join(" ", display);
            }
        }

        if (StringUtils.hasText(aiCorrectedTextZh)) {
            return aiCorrectedTextZh.trim();
        }

        List<String> correctedSequence = new ArrayList<>();
        for (SegmentChoice choice : segmentChoices) {
            if (choice.isSelected()) {
                correctedSequence.add(choice.getSelectedLabel());
            }
        }
        return String.join(" ", correctedSequence);
    }

    private List<SemanticSelectedSegment> buildSelectedSegments(
            SemanticCorrectionRequest request,
            List<String> rawLabels,
            List<SegmentChoice> segmentChoices) {
        List<SemanticSelectedSegment> selectedSegments = new ArrayList<>();

        for (SegmentChoice choice : segmentChoices) {
            SemanticSelectedSegment selectedSegment = new SemanticSelectedSegment();
            selectedSegment.setSegmentIndex(resolveSegmentIndex(request, choice.getRawPosition()));
            selectedSegment.setRawLabel(choice.getRawLabel());
            selectedSegment.setSelectedLabel(choice.getSelectedLabel());
            selectedSegment.setAction(choice.getAction());
            selectedSegments.add(selectedSegment);
        }

        return selectedSegments;
    }

    private List<SemanticRemovedSegment> buildRemovedSegments(
            SemanticCorrectionRequest request,
            List<String> rawLabels,
            List<SegmentChoice> segmentChoices,
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

        for (SegmentChoice choice : segmentChoices) {
            if (choice.isSelected()) {
                continue;
            }

            Integer segmentIndex = resolveSegmentIndex(request, choice.getRawPosition());
            SemanticRemovedSegment aiRemoved = aiRemovedByIndex.get(segmentIndex);

            SemanticRemovedSegment removedSegment = new SemanticRemovedSegment();
            removedSegment.setSegmentIndex(segmentIndex);
            removedSegment.setRawLabel(choice.getRawLabel());
            removedSegment.setRawLabelZh(resolveRawLabelZh(request, choice.getRawPosition(), choice.getRawLabel()));
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

    private List<SegmentChoice> alignSegmentChoices(
            SemanticCorrectionRequest request,
            List<String> rawLabels,
            List<String> correctedSequence) {
        if (correctedSequence.size() > rawLabels.size()) {
            return null;
        }

        List<SegmentChoice> choices = new ArrayList<>();
        int correctedCursor = 0;

        for (int rawPosition = 0; rawPosition < rawLabels.size(); rawPosition++) {
            String rawLabel = rawLabels.get(rawPosition);
            String selectedLabel = null;
            String action = "remove";

            if (correctedCursor < correctedSequence.size()) {
                String candidate = correctedSequence.get(correctedCursor);
                if (isAllowedForSegment(request, rawPosition, rawLabel, candidate)) {
                    selectedLabel = candidate;
                    action = rawLabel.equals(candidate) ? "keep" : "replace";
                    correctedCursor++;
                }
            }

            choices.add(new SegmentChoice(rawPosition, rawLabel, selectedLabel, action));
        }

        if (correctedCursor != correctedSequence.size()) {
            return null;
        }

        return choices;
    }

    private boolean isAllowedForSegment(
            SemanticCorrectionRequest request,
            int rawPosition,
            String rawLabel,
            String selectedLabel) {
        if (!StringUtils.hasText(selectedLabel)) {
            return false;
        }

        String normalizedSelected = selectedLabel.trim();
        if (normalizedSelected.equals(rawLabel)) {
            return true;
        }

        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments == null || rawPosition >= segments.size()) {
            return false;
        }

        SentenceSegmentResult segment = segments.get(rawPosition);
        if (segment == null || segment.getTopK() == null) {
            return false;
        }

        for (SentenceSegmentTopKItem item : segment.getTopK()) {
            if (item != null
                    && StringUtils.hasText(item.getLabel())
                    && normalizedSelected.equals(item.getLabel().trim())) {
                return true;
            }
        }

        return false;
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

    private String resolveSelectedLabelZh(
            SemanticCorrectionRequest request,
            int rawPosition,
            String selectedLabel) {
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && rawPosition < segments.size()) {
            SentenceSegmentResult segment = segments.get(rawPosition);
            if (segment != null) {
                if (selectedLabel.equals(segment.getRawLabel())
                        && StringUtils.hasText(segment.getRawLabelZh())) {
                    return segment.getRawLabelZh().trim();
                }
                if (segment.getTopK() != null) {
                    for (SentenceSegmentTopKItem item : segment.getTopK()) {
                        if (item != null
                                && StringUtils.hasText(item.getLabel())
                                && selectedLabel.equals(item.getLabel().trim())
                                && StringUtils.hasText(item.getLabelZh())) {
                            return item.getLabelZh().trim();
                        }
                    }
                }
            }
        }

        return selectedLabel;
    }

    private static class SegmentChoice {
        private final int rawPosition;
        private final String rawLabel;
        private final String selectedLabel;
        private final String action;

        SegmentChoice(int rawPosition, String rawLabel, String selectedLabel, String action) {
            this.rawPosition = rawPosition;
            this.rawLabel = rawLabel;
            this.selectedLabel = selectedLabel;
            this.action = action;
        }

        int getRawPosition() {
            return rawPosition;
        }

        String getRawLabel() {
            return rawLabel;
        }

        String getSelectedLabel() {
            return selectedLabel;
        }

        String getAction() {
            return action;
        }

        boolean isSelected() {
            return selectedLabel != null;
        }
    }
}
