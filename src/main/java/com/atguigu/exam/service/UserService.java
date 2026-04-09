package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.User;
import com.atguigu.exam.vo.LoginRequestVo;
import com.atguigu.exam.vo.RegisterRequestVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service接口
 * 定义用户相关的业务方法
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param loginVo 登录请求参数 (包含用户名和密码)
     * @return 返回登录结果 (成功包含用户信息，失败包含错误提示)
     */
    Result login(LoginRequestVo loginVo);

    /**
     * 用户注册
     * @param registerVo 注册请求参数 (包含用户名、密码、昵称、角色、学号/工号)
     * @return 返回注册结果
     */
    Result register(RegisterRequestVo registerVo);

    /**
     * 老师查询自己名下的所有学生
     * @param teacherId 老师的ID
     * @return 返回学生列表
     */
    Result getMyStudents(Integer teacherId);
}