package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "开始考试请求参数")
public class StartExamVo {
    
    @Schema(description = "试卷ID，指定要参加的考试试卷", 
            example = "1", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    @Schema(description = "考生姓名", 
            example = "张三", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "考生姓名不能为空")
    private String studentName;

    @Schema(description = "考生ID，用于唯一标识考试用户",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "考生ID不能为空")
    private Integer userId;
}