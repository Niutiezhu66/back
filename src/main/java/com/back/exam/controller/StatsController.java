package com.back.exam.controller;


import com.back.exam.common.Result;
import com.back.exam.service.StatsService;
import com.back.exam.vo.StatsVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
} 