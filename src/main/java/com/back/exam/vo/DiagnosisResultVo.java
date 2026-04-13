package com.back.exam.vo;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisResultVo {
    // 综合健康度分数 (0-100)
    private Integer healthScore;
    // 优势领域
    private List<String> strongPoints;
    // 弱势领域
    private List<String> weakPoints;
    // 雷达图分类及得分数据
    private List<CategoryScore> radarData;
    // 典型盲区错题
    private List<BlindSpot> blindSpots;
    // AI 给出的 Markdown 提升建议
    private String aiSuggestion;

    @Data
    public static class CategoryScore {
        private String name;
        private Integer score; // 该分类的百分制得分率
    }

    @Data
    public static class BlindSpot {
        private String title;        // 盲区标题（知识点）
        private String description;  // 题目描述
        private String errorExample; // 考生的错误答案
    }
}