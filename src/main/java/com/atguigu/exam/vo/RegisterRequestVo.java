package com.atguigu.exam.vo;

import lombok.Data;

@Data
public class RegisterRequestVo {
    private String username;
    private String password;
//    private String nickname;
    private Integer role; // 1-教师, 2-学生
    private Integer userId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}