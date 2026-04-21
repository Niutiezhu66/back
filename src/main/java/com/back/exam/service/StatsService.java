package com.back.exam.service;


import com.back.exam.vo.DiagnosisResultVo;
import com.back.exam.vo.StatsVo;
import com.back.exam.vo.TeacherStatsVo;

public interface StatsService {

    StatsVo getSystemStats();
    DiagnosisResultVo generateAIDiagnosis(Long userId) throws InterruptedException;
    TeacherStatsVo generateTeacherOverview(Long teacherId);
    TeacherStatsVo generateTeacherDiagnosis(Long teacherId) throws InterruptedException;
} 