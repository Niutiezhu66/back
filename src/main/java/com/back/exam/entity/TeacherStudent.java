package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teacher_student")
@Schema(description = "师生关联关系")
public class TeacherStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "教师的数据库主键ID")
    private Long teacherId;

    @Schema(description = "学生的数据库主键ID")
    private Long studentId;

    @Schema(description = "绑定时间")
    private LocalDateTime createTime;
}