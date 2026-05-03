package com.yezhen.hearbridge.backend.controller;

import com.yezhen.hearbridge.backend.dto.SemanticCorrectionRequest;
import com.yezhen.hearbridge.backend.dto.SemanticCorrectionResult;
import com.yezhen.hearbridge.backend.service.SignSemanticCorrectionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手语句子识别 AI 候选排序与忠实自然化 Controller。
 */
@RestController
@RequestMapping("/app/sign-video")
public class SignSemanticCorrectionController {

    private final SignSemanticCorrectionService signSemanticCorrectionService;

    public SignSemanticCorrectionController(SignSemanticCorrectionService signSemanticCorrectionService) {
        this.signSemanticCorrectionService = signSemanticCorrectionService;
    }

    /**
     * 对句子视频原始识别结果进行 AI 候选排序与忠实自然化。
     *
     * @param request 原始识别结果
     * @return AI 候选排序与忠实自然化结果
     */
    @PostMapping("/semantic-correct")
    public SemanticCorrectionResult semanticCorrect(@RequestBody SemanticCorrectionRequest request) {
        return signSemanticCorrectionService.correct(request);
    }
}
