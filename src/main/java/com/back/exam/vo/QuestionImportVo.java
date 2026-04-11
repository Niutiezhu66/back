package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

//题目批量导入

@Data
@Schema(description = "题目导入数据传输对象")
public class QuestionImportVo {
    
    @Schema(description = "题目标题内容", 
            example = "以下关于Java面向对象编程的说法正确的是？", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title; // 题目标题
    
    @Schema(description = "题目类型", 
            example = "CHOICE", 
            allowableValues = {"CHOICE", "JUDGE", "TEXT"})
    private String type;

    @Schema(description = "是否为多选题（仅选择题有效）", 
            example = "false")
    private Boolean multi;
    
    @Schema(description = "题目分类ID", 
            example = "1")
    private Long categoryId;
    
    @Schema(description = "分类名称（仅用于显示，不会保存到数据库）", 
            example = "Java基础")
    private String categoryName;
    
    @Schema(description = "题目难度级别", 
            example = "MEDIUM", 
            allowableValues = {"EASY", "MEDIUM", "HARD"})
    private String difficulty;
    
    @Schema(description = "题目默认分值", 
            example = "5")
    private Integer score;
    
    @Schema(description = "题目解析说明", 
            example = "Java是面向对象编程语言，支持封装、继承、多态三大特性...")
    private String analysis;
    
    @Schema(description = "选择题选项列表（仅选择题需要）")
    private List<ChoiceImportDto> choices;
    
    @Schema(description = "题目答案（判断题和简答题使用）", 
            example = "正确")
    private String answer;
    
    @Schema(description = "答题关键词（用于简答题AI评分）", 
            example = "面向对象,封装,继承,多态")
    private String keywords;
    
//xuanxiang
    @Data
    @Schema(description = "选择题选项数据")
    public static class ChoiceImportDto {
        
        @Schema(description = "选项内容", 
                example = "Java支持多重继承", 
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @Schema(description = "是否为正确答案", 
                example = "false")
        private Boolean isCorrect;
        
        @Schema(description = "选项排序序号", 
                example = "1")
        private Integer sort;
    }
} 