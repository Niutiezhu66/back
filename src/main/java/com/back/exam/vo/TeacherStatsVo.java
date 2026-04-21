package com.back.exam.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TeacherStatsVo {
    private Integer totalExams; // 总考试场次
    private Integer totalStudents; // 参加考试总人数
    private Double avgScore; // 平均分
    private Double passRate; // 及格率
    private List<Map<String, Object>> scoreDistribution; // 分数段分布 (name, value)
    private List<Map<String, Object>> weakPoints; // 薄弱知识点错误率 (name, value)
    private String aiReport; // AI教学报告
}