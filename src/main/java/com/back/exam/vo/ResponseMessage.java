package com.back.exam.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResponseMessage implements Serializable {

    private String role;

    private String content;

    private static final long serialVersionUID = 1L; // 序列化版本UID
} 