package com.back.exam.service;


import com.back.exam.vo.DiagnosisResultVo;
import com.back.exam.vo.StatsVo;

public interface StatsService {

    StatsVo getSystemStats();
    DiagnosisResultVo generateAIDiagnosis(Long userId) throws InterruptedException;
} 