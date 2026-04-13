package com.back.exam.service;


import com.back.exam.entity.Question;
import com.back.exam.vo.AiGenerateRequestVo;
import lombok.Data;

import java.util.List;

public interface KimiAiService {
    String buildPrompt(AiGenerateRequestVo request);

    String callKimiAI(String prompt) throws InterruptedException;

    String buildGradingPrompt(Question question, String userAnswer, Integer maxScore);

    String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount);

    /**
     * 构建详细的考试总评提示词（包含答题详情、题目、分类）
     *
     * @param totalScore 总得分
     * @param maxScore 满分
     * @param questionCount 题目总数
     * @param correctCount 正确题数
     * @param examDetails 考试详细答题情况（包含题目、分类、答题情况）
     * @return AI提示词
     */
    String buildDetailedSummaryPrompt(Integer totalScore, Integer maxScore,
                                       Integer questionCount, Integer correctCount,
                                       List<ExamQuestionDetail> examDetails);

    /**
     * 考试题目详情DTO（用于AI总评）
     */
    @Data
    class ExamQuestionDetail {
        /** 题号 */
        private Integer number;
        /** 题目内容 */
        private String title;
        /** 题目类型：CHOICE/JUDGE/TEXT */
        private String type;
        /** 所属分类名称 */
        private String categoryName;
        /** 难度：EASY/MEDIUM/HARD */
        private String difficulty;
        /** 分值 */
        private Integer score;
        /** 学生答案 */
        private String userAnswer;
        /** 正确答案 */
        private String correctAnswer;
        /** 是否答对：0错误 1正确 2部分正确 */
        private Integer isCorrect;
        /** 该题得分 */
        private Integer obtainedScore;
        /** AI批改意见（简答题） */
        private String aiCorrection;
    }
}