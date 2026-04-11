package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
@Schema(description = "用户登录请求参数")
public class LoginRequestVo {
    
    @Schema(description = "用户名", 
            example = "admin", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Schema(description = "登录密码", 
            example = "123456", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;
} 