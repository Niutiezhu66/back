package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

//答案

@Data
@TableName("question_answers")
@Schema(description = "题目答案信息")
public class QuestionAnswer  extends BaseEntity{

    @Schema(description = "关联的题目ID", 
            example = "1")
    private Long questionId;
    
    @Schema(description = "标准答案内容", 
            example = "正确")
    private String answer;
    
    @Schema(description = "评分关键词，用于简答题AI评分", 
            example = "面向对象,封装,继承,多态")
    private String keywords;
} 