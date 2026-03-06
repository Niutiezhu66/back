package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    @NotNull
    private Long id;

    @Schema(description = "创建时间")
    private Date createTime;

    @JsonIgnore
    @Schema(description = "修改时间")
    private Date updateTime;

    @JsonIgnore
    @Schema(description = "逻辑删除")
    @TableLogic //逻辑删除字段(添加这个注解，mp会忽略已经删除的.0:未删除；1:已删除)
    @TableField("is_deleted")
    private Byte isDeleted;

}