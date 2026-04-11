package com.back.exam.entity;

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
    @TableLogic
    @TableField("is_deleted")
    private Byte isDeleted;

}