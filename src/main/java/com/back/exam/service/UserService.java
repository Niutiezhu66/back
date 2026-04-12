package com.back.exam.service;

import com.back.exam.common.Result;
import com.back.exam.entity.User;
import com.back.exam.vo.LoginRequestVo;
import com.back.exam.vo.RegisterRequestVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.back.exam.vo.TeacherStudentVO;
import java.util.List;

public interface UserService extends IService<User> {
    Result login(LoginRequestVo loginVo);

    Result register(RegisterRequestVo registerVo);

    Result getMyStudents(Integer teacherId);

    // 处理Excel批量导入用户
    Result<String> importUsers(org.springframework.web.multipart.MultipartFile file);

    // 教师根据学号绑定学生
    Result<String> bindStudentByUserId(Long teacherId, Integer studentUserId);

    // 解除师生绑定关系
    Result<String> unbindStudent(Long teacherId, Long studentId);

    Result<List<TeacherStudentVO>> getAllTeacherStudentRelations();

    boolean deleteUserAndRelations(Long id);
}