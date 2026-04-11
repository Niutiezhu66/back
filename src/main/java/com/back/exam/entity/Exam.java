package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exams")
public class Exam extends BaseEntity {
    
    private String name;
    
    private String description;
    
    private Integer duration;
    
    @TableField("pass_score")
    private Integer passScore;
    
    @TableField("total_score")
    private Integer totalScore;
    
    @TableField("question_count")
    private Integer questionCount;
    
    private String status;

} 