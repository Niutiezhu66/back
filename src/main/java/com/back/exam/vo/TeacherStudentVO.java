package com.back.exam.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeacherStudentVO {
    private Long id; // 关联表(teacher_student)的主键ID

    private Long teacherId; // 教师的数据库主键ID
    private String teacherName; // 教师真实姓名
    private Integer teacherUserId; // 教师工号

    private Long studentId; // 学生的数据库主键ID
    private String studentName; // 学生真实姓名
    private Integer studentUserId; // 学生学号

    private LocalDateTime createTime; // 绑定时间
}