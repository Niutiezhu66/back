package com.atguigu.exam.controller;


import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.service.UserService;
import com.atguigu.exam.vo.LoginRequestVo;
import com.atguigu.exam.vo.LoginResponseVo;
import com.atguigu.exam.vo.RegisterRequestVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
/**
 * 用户控制器 - 处理用户认证和权限管理相关的HTTP请求
 * 包括用户登录、权限验证等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/user")  // 用户API路径前缀
@CrossOrigin(origins = "*")  // 允许跨域访问
@Tag(name = "用户管理", description = "用户相关操作，包括登录认证、权限验证等功能")  // Swagger API分组
public class UserController {

    @Autowired
    private UserService userService;

    // 1. 登录接口
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequestVo loginVo) {
        return userService.login(loginVo);
    }

    // 2. 注册接口
//    @PostMapping("/register")
//    public Result register(@RequestBody RegisterRequestVo registerVo) {
//        return userService.register(registerVo);
//    }
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，包含用户名、学号查重和非空校验")
    public Result<String> register(@RequestBody Map<String, Object> registerData) {
        // 1. 获取前端传来的参数
        String username = (String) registerData.get("username");
        String password = (String) registerData.get("password");
        String nickname = (String) registerData.get("nickname");
        Integer role = (Integer) registerData.get("role");

        // 提取前端的 userId（学号/工号）
        Object userIdObj = registerData.get("userId");
        String userIdStr = userIdObj != null ? String.valueOf(userIdObj) : null;

        // 2. 严格非空校验
        if (!StringUtils.hasText(username)) {
            return Result.error("用户名（账号）不能为空！");
        }
        if (!StringUtils.hasText(password)) {
            return Result.error("密码不能为空！");
        }
        if (!StringUtils.hasText(userIdStr)) {
            return Result.error("学号/工号不能为空！");
        }

        // ✨ 修改点：这里改为 Integer
        Integer userIdValue;
        try {
            // 将字符串转换为 Integer 类型
            userIdValue = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return Result.error("学号/工号格式不正确，必须为数字！");
        }

        // 3. 用户名查重
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, username);
        if (userService.count(usernameWrapper) > 0) {
            return Result.error("该用户名已被注册，请更换一个账号！");
        }

        // 4. 学号/工号（userId）查重
        LambdaQueryWrapper<User> userIdWrapper = new LambdaQueryWrapper<>();
        userIdWrapper.eq(User::getUserId, userIdValue);
        if (userService.count(userIdWrapper) > 0) {
            return Result.error("该学号/工号已被注册，请勿重复注册！");
        }

        // 5. 校验全部通过，构建对象并保存
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(nickname);
        user.setRole(role != null ? role.toString() : "2");
        user.setUserId(userIdValue); // 设置用户的业务ID(学号/工号)
        user.setStatus("active");

        userService.save(user);

        return Result.success("注册成功");
    }

    // 3. 教师查询名下学生列表
    @GetMapping("/myStudents")
    public Result getMyStudents(@RequestParam Integer teacherId) {
        return userService.getMyStudents(teacherId);
    }

    /**
     * 用户登录
     * @param loginRequestVo 登录请求
     * @return 登录结果
     */
//    @PostMapping("/login")  // 处理POST请求
//    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录验证，返回用户信息和token")  // API描述
//    public Result<LoginResponseVo> login(@RequestBody LoginRequestVo loginRequestVo) {
//        return Result.success(null);
//    }
//
    /**
     * 检查用户权限
     * @param userId 用户ID
     * @return 权限检查结果
     */
    @GetMapping("/check-admin/{userId}")  // 处理GET请求
    @Operation(summary = "检查管理员权限", description = "验证指定用户是否具有管理员权限")  // API描述
    public Result<Boolean> checkAdmin(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        return Result.success(true);
    }
} 