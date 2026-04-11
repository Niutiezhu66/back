package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "登录成功响应数据")
public class LoginResponseVo {
    
    @Schema(description = "用户ID", 
            example = "1")
    private Long userId;
    @Schema(description = "用户名", 
            example = "admin")
    private String username;
    
    @Schema(description = "用户真实姓名", 
            example = "管理员")
    private String realName;
    
    @Schema(description = "用户角色", 
            example = "ADMIN", 
            allowableValues = {"ADMIN", "TEACHER", "STUDENT"})
    private String role;
    
    @Schema(description = "登录令牌，用于后续API调用的身份验证",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
} 