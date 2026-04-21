package com.back.exam.vo;

import lombok.Data;

@Data
public class ChangePasswordVo {
    private Long id;
    private String oldPassword;
    private String newPassword;
}
