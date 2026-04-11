package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("notices")
@Schema(description = "系统公告信息")
public class Notice extends BaseEntity {

    @Schema(description = "公告标题", 
            example = "系统维护通知")
    private String title;

    @Schema(description = "公告内容详情", 
            example = "系统将于今晚22:00-24:00进行维护升级，期间无法访问，请合理安排考试时间...")
    private String content;
    
    @Schema(description = "公告类型", 
            example = "SYSTEM", 
            allowableValues = {"SYSTEM", "FEATURE", "NOTICE"})
    private String type;
    
    @Schema(description = "优先级级别", 
            example = "1", 
            allowableValues = {"0", "1", "2"})
    private Integer priority;
    
    @Schema(description = "是否启用显示", 
            example = "true")
    private Boolean isActive;

} 