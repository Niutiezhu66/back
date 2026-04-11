package com.back.exam.service;


import com.back.exam.entity.Question;
import com.back.exam.vo.AiGenerateRequestVo;

public interface KimiAiService {
    String buildPrompt(AiGenerateRequestVo request);

    String callKimiAI(String prompt) throws InterruptedException;

    String buildGradingPrompt(Question question, String userAnswer, Integer maxScore);

    String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount);
}