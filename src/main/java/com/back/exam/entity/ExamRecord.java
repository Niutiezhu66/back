package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@TableName(value ="exam_records")
@Data
@Schema(description = "考试记录信息")
public class ExamRecord extends BaseEntity {

    @Schema(description = "考生用户ID",
            example = "1001")
    private Long userId;

    @Schema(description = "试卷ID，关联的考试试卷", 
            example = "1")
    private Integer examId;

    @Schema(description = "考生姓名", 
            example = "张三")
    private String studentName;

    @Schema(description = "考试得分", 
            example = "85")
    private Integer score;

    @Schema(description = "答题记录，JSON格式存储所有答题内容", 
            example = "[{\"questionId\":1,\"userAnswer\":\"A\"},{\"questionId\":2,\"userAnswer\":\"B\"}]")
    private String answers;

    @Schema(description = "考试开始时间", 
            example = "2024-01-15 09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "考试结束时间", 
            example = "2024-01-15 11:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "考试状态", 
            example = "已批阅", 
            allowableValues = {"进行中", "已完成", "已批阅"})
    private String status;

    @Schema(description = "窗口切换次数，用于监控考试过程中的异常行为", 
            example = "2")
    private Integer windowSwitches;


    @Schema(description = "详细的答题记录列表，包含每题的答案和得分情况")
    @TableField(exist = false)
    private List<AnswerRecord> answerRecords;

    @Schema(description = "关联的试卷信息，包含试卷详细内容和题目")
    @TableField(exist = false)
    private Paper paper;

} 