package com.yezhen.hearbridge.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yezhen.hearbridge.backend.config.DeepSeekProperties;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * DeepSeek OpenAI-compatible Chat Completions 客户端。
 */
@Service
public class DeepSeekSemanticClient {

    private static final String SYSTEM_PROMPT = """
            You are a conservative deletion-only post-processor for a sign-language recognition system.

            The recognizer provides:
            - rawSequence: the original predicted gloss sequence.
            - rawTextZh: Chinese display text generated from rawSequence.
            - segmentTopK: ordered recognition segments.
            - each segment has rawLabel and topK visual candidates.

            Your task:
            Clean the rawSequence only by removing obviously extra inserted segments.

            Important:
            This is NOT a maximum-probability reranking task.
            Do NOT choose the highest-probability candidate automatically.
            The rawLabel is the default trusted output.

            Strict rules:
            1. Default behavior: keep every segment's rawLabel.
            2. You may ONLY remove segments. Do not replace labels.
            3. Do not add new segments.
            4. Do not reorder segments.
            5. Do not invent words.
            6. Do not translate into natural English. Keep gloss words only.
            7. Remove a segment only if it is clearly an extra insertion, duplicate, transition noise, or makes the sequence obviously unnatural.
            8. If uncertain, keep the rawSequence unchanged.
            9. Avoid adjacent duplicated words unless repetition is clearly meaningful.
            10. correctedSequence must contain only labels from kept rawLabel values.
            11. correctedTextZh should be a simple Chinese display string corresponding to correctedSequence.
            12. Return JSON only.

            Good correction examples:
            - ["teacher", "learn", "help", "you"] -> remove "learn" -> ["teacher", "help", "you"]
            - ["you", "want", "work", "help"] -> remove "work" -> ["you", "want", "help"]
            - ["please", "meet", "teacher", "learn"] -> remove "learn" -> ["please", "meet", "teacher"]

            Bad corrections:
            - Do NOT change ["you", "want", "work", "help"] into ["you", "work", "work", "help"].
            - Do NOT choose a candidate just because it has higher probability.
            - Do NOT add missing words.
            - Do NOT output a natural English sentence.

            Return format:
            {
              "correctedSequence": ["word1", "word2"],
              "correctedTextZh": "中文 展示",
              "correctionApplied": true,
              "selectedSegments": [
                {
                  "segmentIndex": 1,
                  "rawLabel": "word1",
                  "selectedLabel": "word1",
                  "action": "keep"
                },
                {
                  "segmentIndex": 2,
                  "rawLabel": "extra",
                  "selectedLabel": null,
                  "action": "remove"
                }
              ],
              "removedSegments": [
                {
                  "segmentIndex": 2,
                  "rawLabel": "extra",
                  "rawLabelZh": "多余词",
                  "reason": "extra insertion due to overlapping segmentation",
                  "reasonZh": "该词段可能是滑窗重叠产生的异常插入词。"
                }
              ],
              "reason": "brief English explanation",
              "reasonZh": "简短中文说明"
            }
            """;

    private final DeepSeekProperties deepSeekProperties;

    private final ObjectMapper objectMapper;

    /**
     * JDK HttpClient，避免额外引入 WebFlux 或新的 HTTP 依赖。
     */
    private final HttpClient httpClient;

    public DeepSeekSemanticClient(
            DeepSeekProperties deepSeekProperties,
            ObjectMapper objectMapper) {
        this.deepSeekProperties = deepSeekProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                .build();
    }

    /**
     * 调用 DeepSeek 获取语义修正 JSON。
     *
     * @param request 原始识别结果
     * @return DeepSeek 返回内容解析出的 JSON 节点
     */
    public JsonNode correct(SemanticCorrectionRequest request) {
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(buildRequestBody(request));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getNormalizedBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekProperties.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "DeepSeek 调用失败，HTTP " + response.statusCode() + "：" + abbreviate(response.body())
                );
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String content = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("DeepSeek 返回内容为空");
            }

            return objectMapper.readTree(cleanJsonContent(content));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek 调用被中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("DeepSeek 调用或 JSON 解析失败", exception);
        }
    }

    private ObjectNode buildRequestBody(SemanticCorrectionRequest request) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();

        body.put("model", StringUtils.hasText(deepSeekProperties.getModel())
                ? deepSeekProperties.getModel().trim()
                : "deepseek-v4-flash");
        body.put("temperature", 0);
        body.put("stream", false);
        body.putObject("thinking").put("type", "disabled");

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT);
        messages.addObject()
                .put("role", "user")
                .put("content", "INPUT:\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

        return body;
    }

    private String getNormalizedBaseUrl() {
        String baseUrl = deepSeekProperties.getBaseUrl();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("DeepSeek API 地址未配置");
        }

        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
    }

    private int resolveTimeoutSeconds() {
        Integer timeoutSeconds = deepSeekProperties.getTimeoutSeconds();
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return 30;
        }
        return timeoutSeconds;
    }

    private String cleanJsonContent(String content) {
        String cleaned = content.trim();

        if (cleaned.startsWith("```")) {
            int firstLineEnd = cleaned.indexOf('\n');
            if (firstLineEnd >= 0) {
                cleaned = cleaned.substring(firstLineEnd + 1).trim();
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }

        return cleaned;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }
}
