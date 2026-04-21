package com.back.exam.controller;


import com.back.exam.common.Result;
import com.back.exam.service.StatsService;
import com.back.exam.vo.DiagnosisResultVo;
import com.back.exam.vo.StatsVo;
import com.back.exam.vo.TeacherStatsVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
@Tag(name = "数据统计", description = "系统统计相关操作，包括概览数据、图表统计等功能")
public class StatsController {


    @Autowired
    private StatsService statsService;


    @GetMapping("/overview")
    @Operation(summary = "获取系统概览统计", description = "获取系统的概览统计数据，包括题目、用户、考试等各项数量统计")  // API描述
    public Result<StatsVo> getSystemStats() {
        StatsVo stats = statsService.getSystemStats();
        return Result.success(stats);
    }

    @GetMapping("/test")
    @Operation(summary = "测试数据库连接", description = "测试系统数据库连接状态，用于系统健康检查")  // API描述
    public Result<String> testDatabase() {
        try {

            StatsVo stats = statsService.getSystemStats();
            return Result.success("数据库连接正常，统计数据：" + stats.toString());
        } catch (Exception e) {
            return Result.error("数据库连接测试异常：" + e.getMessage());
        }
    }

    @GetMapping("/diagnosis/{userId}")
    public Result<DiagnosisResultVo> getAIDiagnosis(@PathVariable("userId") Long userId) {
        try {
            DiagnosisResultVo result = statsService.generateAIDiagnosis(userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 诊断生成失败：" + e.getMessage());
        }
    }

    @GetMapping("/teacher/{teacherId}/overview")
    @Operation(summary = "教师工作台概览", description = "获取教师名下考试数据的聚合统计，不触发AI诊断")
    public Result<TeacherStatsVo> getTeacherOverview(@PathVariable("teacherId") Long teacherId) {
        try {
            TeacherStatsVo result = statsService.generateTeacherOverview(teacherId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("教师概览获取失败：" + e.getMessage());
        }
    }

    @GetMapping("/teacher/{teacherId}")
    @Operation(summary = "教师学情诊断", description = "获取教师名下所有考试的数据聚合及AI建议")
    public Result<TeacherStatsVo> getTeacherDiagnosis(@PathVariable("teacherId") Long teacherId) {
        try {
            TeacherStatsVo result = statsService.generateTeacherDiagnosis(teacherId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 教学诊断生成失败：" + e.getMessage());
        }
    }
} 