package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@TableName("question_choices")
@Schema(description = "题目选项信息")
public class QuestionChoice extends BaseEntity{

    @Schema(description = "关联的题目ID", 
            example = "1")
    private Long questionId;
    
    @Schema(description = "选项内容", 
            example = "Java是面向对象编程语言")
    private String content;
    
    @Schema(description = "是否为正确答案", 
            example = "true")
    private Boolean isCorrect;
    
    @Schema(description = "选项排序序号", 
            example = "1")
    private Integer sort;
} 