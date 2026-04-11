package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@TableName(value ="paper_question")
@Data
@NoArgsConstructor
public class PaperQuestion extends BaseEntity {


    private Integer paperId;

    private Long questionId;

    private BigDecimal score;

    public PaperQuestion(Integer paperId, Long questionId, BigDecimal score) {
        this.paperId = paperId;
        this.questionId = questionId;
        this.score = score;
    }
} 