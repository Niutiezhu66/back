package com.back.exam.mapper;

import com.back.exam.entity.TeacherStudent;
import com.back.exam.vo.TeacherStudentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TeacherStudentMapper extends BaseMapper<TeacherStudent> {

    // 连表查询所有师生绑定关系 (注意这里把表名改成了 users)
    @Select("SELECT " +
            "ts.teacher_id AS teacherId, t.real_name AS teacherName, t.user_id AS teacherUserId, " +
            "ts.student_id AS studentId, s.real_name AS studentName, s.user_id AS studentUserId, " +
            "ts.create_time AS createTime " +
            "FROM teacher_student ts " +
            "LEFT JOIN users t ON ts.teacher_id = t.user_id " +
            "LEFT JOIN users s ON ts.student_id = s.user_id " +
            "ORDER BY ts.create_time DESC")
    List<TeacherStudentVO> selectAllRelations();
}