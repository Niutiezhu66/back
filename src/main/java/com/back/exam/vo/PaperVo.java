package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;


@Data
@Schema(description = "试卷创建请求参数")
public class PaperVo implements Serializable {

    @Schema(description = "试卷名称", example = "Java基础知识测试卷", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name; // 试卷名称

    @Schema(description = "试卷描述说明", example = "本试卷主要考察Java基础语法、面向对象编程等知识点")
    private String description; // 试卷描述

    @Schema(description = "考试时长（分钟）", example = "120", minimum = "1", maximum = "600")
    private Integer duration;

    // 新增：接收前端传来的教师ID
    @Schema(description = "教师ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long teacherId;

    @Schema(description = "试卷题目配置，Key为题目ID，Value为该题分数", example = "{\"1\": 5.0, \"2\": 10.0, \"3\": 15.0}")
    private Map<Integer, BigDecimal> questions; // 题目ID及分值

    private static final long serialVersionUID = 1L; // 序列化版本UID
}