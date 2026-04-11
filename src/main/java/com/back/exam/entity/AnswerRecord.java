package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;


@TableName(value ="answer_record")
@Data
@NoArgsConstructor
@Schema(description = "答题记录信息")
public class AnswerRecord extends BaseEntity {

    @Schema(description = "关联的考试记录ID", 
            example = "1")
    private Integer examRecordId;

    @Schema(description = "题目ID", 
            example = "5")
    private Integer questionId;

    @Schema(description = "学生提交的答案", 
            example = "A")
    private String userAnswer;

    @Schema(description = "该题得分", 
            example = "5")
    private Integer score;
    @Schema(description = "答题正确性", 
            example = "1", 
            allowableValues = {"0", "1", "2"})
    private Integer isCorrect;

    @Schema(description = "AI智能批改的评价意见", 
            example = "答案基本正确，但缺少关键概念的解释...")
    private String aiCorrection;


    public AnswerRecord(Integer examRecordId, Integer questionId, String userAnswer) {
        this.examRecordId = examRecordId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
    }
} 