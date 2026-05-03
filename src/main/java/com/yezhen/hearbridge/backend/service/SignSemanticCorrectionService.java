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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 手语句子识别 AI 语义修正服务。
 */
@Service
public class SignSemanticCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(SignSemanticCorrectionService.class);

    private static final int MAX_EDIT_COUNT = 2;

    private static final int MAX_REPLACE_COUNT = 1;

    private static final int MAX_CANDIDATES = 81;

    private static final String FALLBACK_REASON = "fallback to raw sequence";

    private static final String FALLBACK_REASON_ZH = "AI 候选排序与自然化暂不可用，已保留原始识别结果。";

    private final DeepSeekSemanticClient deepSeekSemanticClient;

    private final ObjectMapper objectMapper;

    public SignSemanticCorrectionService(
            DeepSeekSemanticClient deepSeekSemanticClient,
            ObjectMapper objectMapper) {
        this.deepSeekSemanticClient = deepSeekSemanticClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 对识别结果做候选序列排序与忠实自然化。
     *
     * @param request 原始识别结果
     * @return 候选排序与自然化结果；DeepSeek 不可用或输出非法时返回 fallback
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

        List<CandidateSequence> candidates = buildCandidates(safeRequest, rawLabels);
        if (candidates.isEmpty()) {
            return fallback(rawSequence, rawTextZh);
        }

        try {
            Map<String, Object> deepSeekRequest = buildDeepSeekRequest(
                    safeRequest,
                    rawSequence,
                    rawTextZh,
                    candidates
            );
            JsonNode correctionJson = deepSeekSemanticClient.correct(deepSeekRequest);
            SemanticCorrectionResult aiResult = objectMapper.treeToValue(
                    correctionJson,
                    SemanticCorrectionResult.class
            );

            if (aiResult == null || !StringUtils.hasText(aiResult.getSelectedCandidateId())) {
                throw new IllegalStateException("DeepSeek 返回缺少 selectedCandidateId");
            }

            String selectedCandidateId = aiResult.getSelectedCandidateId().trim();
            CandidateSequence selectedCandidate = findCandidate(candidates, selectedCandidateId);
            if (selectedCandidate == null) {
                throw new IllegalStateException("DeepSeek selectedCandidateId 不在候选列表中");
            }

            List<String> correctedSequence = new ArrayList<>(selectedCandidate.getSequence());
            List<String> aiWrittenSequence = normalizeOptionalLabels(aiResult.getCorrectedGlossSequence());
            if (!aiWrittenSequence.isEmpty() && !aiWrittenSequence.equals(correctedSequence)) {
                log.warn(
                        "DeepSeek correctedGlossSequence differs from selected candidate, trust selectedCandidateId: {}",
                        selectedCandidateId
                );
            }

            List<SegmentChoice> segmentChoices = selectedCandidate.getChoices();
            String chineseTranslation = firstText(
                    aiResult.getChineseTranslation(),
                    aiResult.getCorrectedTextZh(),
                    resolveCorrectedTextZh(safeRequest, segmentChoices)
            );
            String englishSentence = firstText(
                    aiResult.getEnglishSentence(),
                    String.join(" ", correctedSequence)
            );
            String translationConfidence = normalizeConfidence(aiResult.getTranslationConfidence());
            boolean isIncomplete = aiResult.getIsIncomplete() != null
                    ? aiResult.getIsIncomplete()
                    : "low".equals(translationConfidence);
            String translationNote = firstText(
                    aiResult.getTranslationNote(),
                    aiResult.getReason(),
                    "DeepSeek candidate ranking completed"
            );

            SemanticCorrectionResult result = new SemanticCorrectionResult();
            result.setRawSequence(rawSequence);
            result.setRawTextZh(rawTextZh);
            result.setSelectedCandidateId(selectedCandidateId);
            result.setCorrectedGlossSequence(correctedSequence);
            result.setCorrectedSequence(correctedSequence);
            result.setCorrectedTextZh(chineseTranslation);
            result.setEnglishSentence(englishSentence);
            result.setChineseTranslation(chineseTranslation);
            result.setTranslationConfidence(translationConfidence);
            result.setIsIncomplete(isIncomplete);
            result.setTranslationNote(translationNote);
            result.setCorrectionApplied(!correctedSequence.equals(rawLabels));
            result.setSelectedSegments(buildSelectedSegments(safeRequest, segmentChoices));
            result.setRemovedSegments(buildRemovedSegments(safeRequest, segmentChoices));
            result.setReason(firstText(aiResult.getReason(), "DeepSeek candidate ranking completed"));
            result.setReasonZh(translationNote);
            result.setFallback(false);

            return result;
        } catch (Exception exception) {
            log.warn("DeepSeek candidate ranking fallback: {}", exception.getMessage());
            return fallback(rawSequence, rawTextZh);
        }
    }

    private SemanticCorrectionResult fallback(List<String> rawSequence, String rawTextZh) {
        List<String> safeRawSequence = rawSequence == null ? Collections.emptyList() : rawSequence;
        String safeRawTextZh = rawTextZh == null ? "" : rawTextZh;
        boolean isIncomplete = safeRawSequence.isEmpty();

        SemanticCorrectionResult result = new SemanticCorrectionResult();

        result.setRawSequence(safeRawSequence);
        result.setRawTextZh(safeRawTextZh);
        result.setSelectedCandidateId("C000");
        result.setCorrectedGlossSequence(safeRawSequence);
        result.setCorrectedSequence(safeRawSequence);
        result.setCorrectedTextZh(safeRawTextZh);
        result.setEnglishSentence(String.join(" ", safeRawSequence));
        result.setChineseTranslation(safeRawTextZh);
        result.setTranslationConfidence(isIncomplete ? "low" : "medium");
        result.setIsIncomplete(isIncomplete);
        result.setTranslationNote(FALLBACK_REASON_ZH);
        result.setCorrectionApplied(false);
        result.setSelectedSegments(Collections.emptyList());
        result.setRemovedSegments(Collections.emptyList());
        result.setReason(FALLBACK_REASON);
        result.setReasonZh(FALLBACK_REASON_ZH);
        result.setFallback(true);

        return result;
    }

    private Map<String, Object> buildDeepSeekRequest(
            SemanticCorrectionRequest request,
            List<String> rawSequence,
            String rawTextZh,
            List<CandidateSequence> candidates) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rawSequence", rawSequence);
        payload.put("rawTextZh", rawTextZh);
        payload.put("segments", request.getSegmentTopK());

        List<Map<String, Object>> candidatePayload = new ArrayList<>();
        for (CandidateSequence candidate : candidates) {
            candidatePayload.add(candidate.toPayload(request));
        }
        payload.put("candidates", candidatePayload);

        return payload;
    }

    private List<CandidateSequence> buildCandidates(
            SemanticCorrectionRequest request,
            List<String> rawLabels) {
        List<List<SegmentOption>> optionsByPosition = new ArrayList<>();
        for (int index = 0; index < rawLabels.size(); index++) {
            optionsByPosition.add(buildOptionsForSegment(request, index, rawLabels.get(index)));
        }

        List<CandidateSequence> candidates = new ArrayList<>();
        Set<String> seenSequences = new LinkedHashSet<>();
        collectCandidates(
                optionsByPosition,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                0,
                0,
                0,
                candidates,
                seenSequences
        );
        return candidates;
    }

    private List<SegmentOption> buildOptionsForSegment(
            SemanticCorrectionRequest request,
            int rawPosition,
            String rawLabel) {
        List<SegmentOption> options = new ArrayList<>();
        options.add(new SegmentOption(rawLabel, "keep"));
        options.add(new SegmentOption(null, "remove"));

        Set<String> replacementLabels = new LinkedHashSet<>();
        List<SentenceSegmentResult> segments = request.getSegmentTopK();
        if (segments != null && rawPosition < segments.size()) {
            SentenceSegmentResult segment = segments.get(rawPosition);
            if (segment != null && segment.getTopK() != null) {
                for (SentenceSegmentTopKItem item : segment.getTopK()) {
                    if (item == null || !StringUtils.hasText(item.getLabel())) {
                        continue;
                    }
                    String label = item.getLabel().trim();
                    if (!label.equals(rawLabel)) {
                        replacementLabels.add(label);
                    }
                }
            }
        }

        for (String label : replacementLabels) {
            options.add(new SegmentOption(label, "replace"));
        }

        return options;
    }

    private void collectCandidates(
            List<List<SegmentOption>> optionsByPosition,
            int rawPosition,
            List<String> sequence,
            List<SegmentChoice> choices,
            int editCount,
            int replaceCount,
            int removeCount,
            List<CandidateSequence> candidates,
            Set<String> seenSequences) {
        if (candidates.size() >= MAX_CANDIDATES) {
            return;
        }
        if (editCount > MAX_EDIT_COUNT || replaceCount > MAX_REPLACE_COUNT) {
            return;
        }

        if (rawPosition >= optionsByPosition.size()) {
            if (sequence.isEmpty()) {
                return;
            }
            if (editCount > 0 && hasAdjacentDuplicates(sequence)) {
                return;
            }

            String key = String.join("\u0001", sequence);
            if (!seenSequences.add(key)) {
                return;
            }

            String candidateId = String.format("C%03d", candidates.size());
            candidates.add(new CandidateSequence(
                    candidateId,
                    new ArrayList<>(sequence),
                    new ArrayList<>(choices),
                    editCount,
                    replaceCount,
                    removeCount
            ));
            return;
        }

        for (SegmentOption option : optionsByPosition.get(rawPosition)) {
            int nextEditCount = editCount + ("keep".equals(option.getAction()) ? 0 : 1);
            int nextReplaceCount = replaceCount + ("replace".equals(option.getAction()) ? 1 : 0);
            int nextRemoveCount = removeCount + ("remove".equals(option.getAction()) ? 1 : 0);

            if (nextEditCount > MAX_EDIT_COUNT || nextReplaceCount > MAX_REPLACE_COUNT) {
                continue;
            }

            List<String> nextSequence = new ArrayList<>(sequence);
            if (option.getLabel() != null) {
                nextSequence.add(option.getLabel());
            }

            List<SegmentChoice> nextChoices = new ArrayList<>(choices);
            String rawLabel = optionsByPosition.get(rawPosition).get(0).getLabel();
            nextChoices.add(new SegmentChoice(
                    rawPosition,
                    rawLabel,
                    option.getLabel(),
                    option.getAction()
            ));

            collectCandidates(
                    optionsByPosition,
                    rawPosition + 1,
                    nextSequence,
                    nextChoices,
                    nextEditCount,
                    nextReplaceCount,
                    nextRemoveCount,
                    candidates,
                    seenSequences
            );
        }
    }

    private boolean hasAdjacentDuplicates(List<String> sequence) {
        for (int index = 1; index < sequence.size(); index++) {
            if (sequence.get(index).equals(sequence.get(index - 1))) {
                return true;
            }
        }
        return false;
    }

    private CandidateSequence findCandidate(List<CandidateSequence> candidates, String candidateId) {
        for (CandidateSequence candidate : candidates) {
            if (candidate.getCandidateId().equals(candidateId)) {
                return candidate;
            }
        }
        return null;
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
            List<SegmentChoice> segmentChoices) {
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
            List<SegmentChoice> segmentChoices) {
        List<SemanticRemovedSegment> removedSegments = new ArrayList<>();

        for (SegmentChoice choice : segmentChoices) {
            if (choice.isSelected()) {
                continue;
            }

            SemanticRemovedSegment removedSegment = new SemanticRemovedSegment();
            removedSegment.setSegmentIndex(resolveSegmentIndex(request, choice.getRawPosition()));
            removedSegment.setRawLabel(choice.getRawLabel());
            removedSegment.setRawLabelZh(resolveRawLabelZh(
                    request,
                    choice.getRawPosition(),
                    choice.getRawLabel()
            ));
            removedSegment.setReason("candidate sequence removed this segment");
            removedSegment.setReasonZh("候选序列删除了该词段。");
            removedSegments.add(removedSegment);
        }

        return removedSegments;
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

    private String normalizeConfidence(String value) {
        if (!StringUtils.hasText(value)) {
            return "medium";
        }

        String normalized = value.trim().toLowerCase();
        if ("high".equals(normalized) || "medium".equals(normalized) || "low".equals(normalized)) {
            return normalized;
        }

        return "medium";
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return "";
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

    private static class SegmentOption {
        private final String label;
        private final String action;

        SegmentOption(String label, String action) {
            this.label = label;
            this.action = action;
        }

        String getLabel() {
            return label;
        }

        String getAction() {
            return action;
        }
    }

    private static class CandidateSequence {
        private final String candidateId;
        private final List<String> sequence;
        private final List<SegmentChoice> choices;
        private final int editCount;
        private final int replaceCount;
        private final int removeCount;

        CandidateSequence(
                String candidateId,
                List<String> sequence,
                List<SegmentChoice> choices,
                int editCount,
                int replaceCount,
                int removeCount) {
            this.candidateId = candidateId;
            this.sequence = sequence;
            this.choices = choices;
            this.editCount = editCount;
            this.replaceCount = replaceCount;
            this.removeCount = removeCount;
        }

        String getCandidateId() {
            return candidateId;
        }

        List<String> getSequence() {
            return sequence;
        }

        List<SegmentChoice> getChoices() {
            return choices;
        }

        Map<String, Object> toPayload(SemanticCorrectionRequest request) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("candidateId", candidateId);
            payload.put("sequence", sequence);
            payload.put("edits", buildEdits(request));
            payload.put("editCount", editCount);
            payload.put("replaceCount", replaceCount);
            payload.put("removeCount", removeCount);
            payload.put("source", resolveSource());
            return payload;
        }

        private List<Map<String, Object>> buildEdits(SemanticCorrectionRequest request) {
            List<Map<String, Object>> edits = new ArrayList<>();
            for (SegmentChoice choice : choices) {
                if ("keep".equals(choice.getAction())) {
                    continue;
                }

                Map<String, Object> edit = new LinkedHashMap<>();
                edit.put("type", choice.getAction());
                edit.put("segmentIndex", resolveSegmentIndex(request, choice.getRawPosition()));
                edit.put("from", choice.getRawLabel());
                edit.put("to", choice.getSelectedLabel());
                edits.add(edit);
            }
            return edits;
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

        private String resolveSource() {
            if (editCount == 0) {
                return "raw";
            }
            if (replaceCount > 0 && removeCount > 0) {
                return "replace_remove";
            }
            if (replaceCount > 0) {
                return "replace";
            }
            return "remove";
        }
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
