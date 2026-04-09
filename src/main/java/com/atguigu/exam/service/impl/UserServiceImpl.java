package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.mapper.UserMapper;
import com.atguigu.exam.service.UserService;
import com.atguigu.exam.vo.LoginRequestVo;
import com.atguigu.exam.vo.RegisterRequestVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

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