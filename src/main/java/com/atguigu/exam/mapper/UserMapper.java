package com.atguigu.exam.mapper;

import com.atguigu.exam.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    // 查询某位老师名下的所有学生
    @Select("SELECT u.* FROM users u JOIN teacher_student ts ON u.id = ts.student_id WHERE ts.teacher_id = #{teacherId}")
    List<User> selectStudentsByTeacherId(@Param("teacherId") Integer teacherId);

}