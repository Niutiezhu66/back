package com.back.exam.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

//题目类型枚举

public enum QuestionType {
    CHOICE,JUDGE,TEXT;
    @JsonCreator
    public static QuestionType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (QuestionType type : QuestionType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        // throw new IllegalArgumentException("未知的题目类型: " + value);
        return null;
    }
} 