package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("users")
@Schema(description = "用户信息")
public class User extends BaseEntity {
    
    @Schema(description = "用户名，用于登录", 
            example = "admin")
    private String username;
    
    @Schema(description = "用户密码", 
            example = "******")
    private String password;
    
    @Schema(description = "用户真实姓名", 
            example = "张三")
    @TableField("real_name")
    private String realName;
    
    @Schema(description = "用户角色", 
            example = "ADMIN", 
            allowableValues = {"ADMIN", "TEACHER", "STUDENT"})
    private String role;
    
    @Schema(description = "用户状态", 
            example = "ACTIVE", 
            allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;

    private Integer userId;
} 