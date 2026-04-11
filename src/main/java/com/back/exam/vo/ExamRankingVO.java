package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 考试排行榜VO - 排行榜展示专用数据对象
 * 只包含排行榜所需的核心字段，避免查询过多无用数据
 */
@Data
@Schema(description = "考试排行榜信息")
public class ExamRankingVO implements Serializable {

    @Schema(description = "考试记录ID", example = "1")
    private Integer id;

    @Schema(description = "考生姓名", example = "张三")
    private String studentName;

    @Schema(description = "考试得分", example = "85")
    private Integer score;

    @Schema(description = "试卷ID", example = "1")
    private Integer examId;

    @Schema(description = "试卷名称", example = "Java基础知识测试")
    private String paperName; // 通过关联查询获取

    @Schema(description = "试卷总分", example = "100")
    private BigDecimal paperTotalScore; // 试卷总分（通过关联查询获取）

    @Schema(description = "考试开始时间", example = "2024-01-15 09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "考试结束时间", example = "2024-01-15 11:00:00")
    private LocalDateTime endTime;

    @Schema(description = "考试用时（分钟）", example = "120")
    private Long duration;


    @Schema(description = "试卷信息对象")
    public Map<String, Object> getPaper() {
        Map<String, Object> paper = new HashMap<>();
        paper.put("id", this.examId);
        paper.put("name", this.paperName);
        paper.put("totalScore", this.paperTotalScore);
        return paper;
    }

    private static final long serialVersionUID = 1L; // 序列化版本号
} 