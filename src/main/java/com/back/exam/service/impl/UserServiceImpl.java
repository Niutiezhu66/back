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
import com.back.exam.vo.TeacherStudentVO;
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
            int bindCount = 0; // 记录绑定的师生对数

            // 【关键修改】：使用 DataFormatter 替代原来的 getCellValueAsString，完美防止纯数字报错
            DataFormatter formatter = new DataFormatter();

            // 假设Excel表头为: 用户名(0) | 密码(1) | 真实姓名(2) | 角色(3) | 学号/工号(4) | 教师工号(5)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 使用 formatter 读取所有列
                String username = formatter.formatCellValue(row.getCell(0)).trim();
                String password = formatter.formatCellValue(row.getCell(1)).trim();
                String realName = formatter.formatCellValue(row.getCell(2)).trim();
                String roleStr = formatter.formatCellValue(row.getCell(3)).trim();
                String userIdStr = formatter.formatCellValue(row.getCell(4)).trim();
                String teacherUserIdStr = formatter.formatCellValue(row.getCell(5)).trim(); // 第6列

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

                // 1. 保存用户
                User user = new User();
                user.setUsername(username);
                user.setPassword(password);
                user.setRealName(realName);
                String role = roleStr.contains("1") ? "1" : "2";
                user.setRole(role); // 1为老师，2为学生
                user.setUserId(userIdValue);
                user.setStatus("active");
                save(user); // 保存后自动回填主键ID
                successCount++;

                // 2. 【新增】：如果是学生，且填了教师工号，自动绑定师生关系
                if ("2".equals(role) && !teacherUserIdStr.isEmpty()) {
                    try {
                        Integer teacherUserId = Double.valueOf(teacherUserIdStr).intValue();
                        LambdaQueryWrapper<User> teacherWrapper = new LambdaQueryWrapper<>();
                        teacherWrapper.eq(User::getUserId, teacherUserId).eq(User::getRole, "1");
                        User teacher = getOne(teacherWrapper);

                        if (teacher != null) {
                            TeacherStudent relation = new TeacherStudent();
                            relation.setTeacherId(Long.valueOf(teacher.getUserId()));
                            relation.setStudentId(Long.valueOf(user.getUserId()));
                            relation.setCreateTime(LocalDateTime.now());
                            teacherStudentMapper.insert(relation);
                            bindCount++;
                        }
                    } catch (Exception e) {
                        System.out.println("第" + i + "行师生绑定失败，教师工号格式错误或不存在：" + e.getMessage());
                    }
                }
            }
            return Result.success("导入完成！新增: " + successCount + " 人, 绑定师生: " + bindCount + " 对, 跳过重复: " + skipCount + " 条。");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Excel解析失败，请检查文件格式：" + e.getMessage());
        }
    }

    @Override
    public Result<List<TeacherStudentVO>> getAllTeacherStudentRelations() {
        List<TeacherStudentVO> list = teacherStudentMapper.selectAllRelations();
        return Result.success(list);
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
                .eq(TeacherStudent::getStudentId, Long.valueOf(studentUserId));
        if (teacherStudentMapper.selectCount(tsWrapper) > 0) {
            return Result.error("该学生已在您的名下，无需重复绑定！");
        }

        // 3. 建立绑定关系
        TeacherStudent relation = new TeacherStudent();
        relation.setTeacherId(teacherId);
        relation.setStudentId(Long.valueOf(studentUserId));
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

        // 2. 正常查询 (此时会被 MyBatis-Plus 自动拼上 AND is_deleted=0)
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginVo.getUsername())
                .eq("password", loginVo.getPassword());

        User user = this.getOne(wrapper);

        // 3. 如果查不到人
        if (user == null) {
            // 【关键修改】：去查一下是不是因为账号被删除了
            Integer isDeleted = baseMapper.checkUserDeletedStatus(loginVo.getUsername());
            if (isDeleted != null && isDeleted == 1) {
                return Result.error("该账号已被注销或删除，无法登录，请联系管理员！");
            }
            return Result.error("用户名或密码错误");
        }

        if ("disabled".equals(user.getStatus())) {
            return Result.error("账号已被禁用，请联系管理员");
        }

        // 登录成功，将密码置空后再返回给前端
        user.setPassword(null);
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


    @Override
    @Transactional(rollbackFor = Exception.class) // 保证事务，要么都成功，要么都失败
    public boolean deleteUserAndRelations(Long id) {
        // 1. 先查出该用户信息，拿到业务学号/工号
        User user = this.getById(id);
        if (user == null) {
            return false;
        }
        Integer businessUserId = user.getUserId();

        // 2. 逻辑删除 users 表中的用户
        boolean removed = this.removeById(id);

        // 3. 如果删除成功，则去 teacher_student 表中物理删除关联关系
        if (removed && businessUserId != null) {
            Long targetId = Long.valueOf(businessUserId);
            LambdaQueryWrapper<TeacherStudent> tsWrapper = new LambdaQueryWrapper<>();
            // 只要 teacher_id 或 student_id 是这个人，统统删掉
            tsWrapper.eq(TeacherStudent::getTeacherId, targetId)
                    .or()
                    .eq(TeacherStudent::getStudentId, targetId);

            teacherStudentMapper.delete(tsWrapper);
        }

        return removed;
    }
}