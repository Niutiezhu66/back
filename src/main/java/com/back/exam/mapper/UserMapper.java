package com.back.exam.mapper;

import com.back.exam.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    // 查询某位老师名下的所有学生
    @Select("SELECT u.* FROM users u \n" +
            "INNER JOIN teacher_student ts ON u.user_id = ts.student_id  WHERE ts.teacher_id = #{teacherId}")
    List<User> selectStudentsByTeacherId(@Param("teacherId") Integer teacherId);

    @Select("SELECT is_deleted FROM users WHERE username = #{username} LIMIT 1")
    Integer checkUserDeletedStatus(String username);
}