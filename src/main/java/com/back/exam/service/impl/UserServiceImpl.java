package com.back.exam.service.impl;

import com.back.exam.common.Result;
import com.back.exam.entity.User;
import com.back.exam.mapper.UserMapper;
import com.back.exam.service.UserService;
import com.back.exam.vo.LoginRequestVo;
import com.back.exam.vo.RegisterRequestVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.back.exam.entity.TeacherStudent;
import com.back.exam.mapper.TeacherStudentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private TeacherStudentMapper teacherStudentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> importUsers(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传的Excel文件为空！");
        }
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;
            int skipCount = 0;

            // 假设Excel表头为: 用户名(账号) | 密码 | 真实姓名 | 角色(1老师/2学生) | 学号/工号
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String username = getCellValueAsString(row.getCell(0));
                String password = getCellValueAsString(row.getCell(1));
                String realName = getCellValueAsString(row.getCell(2));
                String roleStr = getCellValueAsString(row.getCell(3));
                String userIdStr = getCellValueAsString(row.getCell(4));

                if (username.isEmpty() || password.isEmpty() || userIdStr.isEmpty()) {
                    continue; // 必填字段为空则跳过
                }

                // 查重：用户名或学号是否已存在
                Integer userIdValue = Double.valueOf(userIdStr).intValue();
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getUsername, username).or().eq(User::getUserId, userIdValue);
                if (count(wrapper) > 0) {
                    skipCount++;
                    continue;
                }

                // 保存用户
                User user = new User();
                user.setUsername(username);
                user.setPassword(password);
                user.setRealName(realName);
                user.setRole(roleStr.contains("1") ? "1" : "2"); // 1为老师，2为学生
                user.setUserId(userIdValue);
                user.setStatus("active");
                save(user);
                successCount++;
            }
            return Result.success("导入完成！成功导入: " + successCount + " 条，跳过重复/无效数据: " + skipCount + " 条。");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Excel解析失败，请检查文件格式是否正确。");
        }
    }

    @Override
    public Result<String> bindStudentByUserId(Long teacherId, Integer studentUserId) {
        // 1. 根据前端传来的学号，找到对应的学生记录
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserId, studentUserId).eq(User::getRole, "2");
        User student = getOne(userWrapper);

        if (student == null) {
            return Result.error("未找到该学号对应的学生，请核对学号！");
        }

        // 2. 检查是否已经绑定过
        LambdaQueryWrapper<TeacherStudent> tsWrapper = new LambdaQueryWrapper<>();
        tsWrapper.eq(TeacherStudent::getTeacherId, teacherId)
                .eq(TeacherStudent::getStudentId, student.getId());
        if (teacherStudentMapper.selectCount(tsWrapper) > 0) {
            return Result.error("该学生已在您的名下，无需重复绑定！");
        }

        // 3. 建立绑定关系
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacherId);
        relation.setStudentId(student.getId());
        relation.setCreateTime(LocalDateTime.now());
        teacherStudentMapper.insert(relation);

        return Result.success("成功绑定学生：" + (student.getRealName() != null ? student.getRealName() : student.getUsername()));
    }

    @Override
    public Result<String> unbindStudent(Long teacherId, Long studentId) {
        LambdaQueryWrapper<TeacherStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherStudent::getTeacherId, teacherId)
                .eq(TeacherStudent::getStudentId, studentId);

        int rows = teacherStudentMapper.delete(wrapper);
        if (rows > 0) {
            return Result.success("解除绑定成功！");
        }
        return Result.error("未找到相关绑定关系。");
    }

    // 辅助方法：安全读取Excel单元格内容
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }


    @Override
    public Result login(LoginRequestVo loginVo) {
        // 1. 校验参数是否为空
        if (!StringUtils.hasText(loginVo.getUsername()) || !StringUtils.hasText(loginVo.getPassword())) {
            return Result.error("用户名或密码不能为空");
        }

        // 2. 根据用户名和密码查询数据库
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 注意：实际开发中强烈建议密码进行MD5或其他单向加密比对，此处为了演示简单直接用明文比对
        wrapper.eq("username", loginVo.getUsername())
                .eq("password", loginVo.getPassword());

        User user = this.getOne(wrapper);

        // 3. 判断是否登录成功
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 登录成功，将密码置空后再返回给前端，防止密码泄露
        user.setPassword(null);

        // 可选：如果项目中集成了JWT，这里应该生成Token并与user信息一起返回
        return Result.success(user);
    }

    @Override
    public Result register(RegisterRequestVo registerVo) {
        // 1. 校验必填项
        if (!StringUtils.hasText(registerVo.getUsername()) || !StringUtils.hasText(registerVo.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }

        // 2. 校验用户名是否已被注册
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", registerVo.getUsername());
        if (this.count(wrapper) > 0) {
            return Result.error("该用户名已被注册，请更换一个");
        }

        // 3. 构建用户实体并保存到数据库
        User user = new User();
        user.setUsername(registerVo.getUsername());
        // 实际开发中推荐对密码进行加密，例如： MD5(registerVo.getPassword())
        user.setPassword(registerVo.getPassword());
//        user.setNickname(registerVo.getNickname());

        // 如果前端没有传角色，默认给 2 (学生)
//        user.setRole(registerVo.getRole() != null ? registerVo.getRole() : 2);
        user.setRole(registerVo.getRole() != null ? String.valueOf(registerVo.getRole()) : "2");
        user.setUserId(registerVo.getUserId());

        // 也可以设置一些默认头像之类的
        // user.setAvatar("默认头像URL");

        boolean isSuccess = this.save(user);

        if (isSuccess) {
            return Result.success("注册成功");
        } else {
            return Result.error("系统异常，注册失败");
        }
    }

    @Override
    public Result getMyStudents(Integer teacherId) {
        // 调用我们在 UserMapper 中自定义的 SQL 方法
        // （前提是您已经按照之前的方案在 UserMapper.java 中加上了 @Select 方法）
        List<User> students = baseMapper.selectStudentsByTeacherId(teacherId);

        // 遍历把密码清空，保护隐私
        if (students != null) {
            for (User student : students) {
                student.setPassword(null);
            }
        }

        return Result.success(students);
    }
}