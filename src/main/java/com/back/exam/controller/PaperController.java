package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.Paper;
import com.back.exam.service.PaperService;
import com.back.exam.vo.AiPaperVo;
import com.back.exam.vo.PaperVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/api/papers")
@Tag(name = "试卷管理", description = "试卷相关操作，包括创建、查询、更新、删除，以及AI智能组卷功能")  // Swagger API分组
public class PaperController {
    @Autowired
    private PaperService paperService;


    @GetMapping("/list")
    @Operation(summary = "获取试卷列表", description = "支持按名称模糊搜索和状态筛选的试卷列表查询")
    public Result<java.util.List<Paper>> listPapers(
            @Parameter(description = "试卷名称，支持模糊查询") @RequestParam(required = false) String name,
            @Parameter(description = "试卷状态，可选值：DRAFT/PUBLISHED/STOPPED") @RequestParam(required = false) String status,
            // 👇 这里是必须要加上的两个参数！后端才能接到前端发来的身份信息
            @Parameter(description = "当前登录用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "当前登录用户角色") @RequestParam(required = false) String role) {

        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!ObjectUtils.isEmpty(name), Paper::getName, name);
        queryWrapper.eq(!ObjectUtils.isEmpty(status), Paper::getStatus, status);

        if (role != null && "1".equals(role) && userId != null) {
            queryWrapper.eq(Paper::getTeacherId, userId);
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(Paper::getCreateTime);

        List<Paper> paperList = paperService.list(queryWrapper);
        return Result.success(paperList);
    }


    @PostMapping
    @Operation(summary = "手动创建试卷", description = "通过手动选择题目的方式创建试卷")  // API描述
    public Result<Paper> createPaper(@RequestBody PaperVo paperVo) {
        Paper paper = paperService.createPaper(paperVo);
        return Result.success(paper, "试卷创建成功");
    }


    @PutMapping("/{id}")
    @Operation(summary = "更新试卷信息", description = "更新试卷的基本信息和题目配置")  // API描述
    public Result<Paper> updatePaper(
            @Parameter(description = "试卷ID") @PathVariable Integer id, 
            @RequestBody PaperVo paperVo) {
        Paper paper = paperService.updatePaper(id,paperVo);
        return Result.success(paper, "试卷更新成功");
    }


    @PostMapping("/ai")
    @Operation(summary = "AI智能组卷", description = "基于设定的规则（题型分布、难度配比等）使用AI自动生成试卷")  // API描述
    public Result<Paper> createPaperWithAI(@RequestBody AiPaperVo aiPaperVo) {
        Paper paper = paperService.aiCreatePaper(aiPaperVo);
        return Result.success(paper, "AI智能组卷成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取试卷详情", description = "获取试卷的详细信息，包括试卷基本信息和包含的所有题目")  // API描述
    public Result<Paper> getPaperById(@Parameter(description = "试卷ID") @PathVariable Integer id) {
        Paper paper = paperService.getPaperById(id);
        return Result.success(paper);
    }


    @PostMapping("/{id}/status")
    @Operation(summary = "更新试卷状态", description = "修改试卷状态：发布试卷供学生考试或停止试卷禁止考试")  // API描述
    public Result<Void> updatePaperStatus(
            @Parameter(description = "试卷ID") @PathVariable Integer id, 
            @Parameter(description = "新的状态，可选值：PUBLISHED/STOPPED") @RequestParam String status) {
        LambdaUpdateWrapper<Paper> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Paper::getId, id);
        updateWrapper.set(Paper::getStatus, status);
        paperService.update(updateWrapper);
        return Result.success(null);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "删除试卷", description = "删除指定的试卷，注意：已发布的试卷不能删除")  // API描述
    public Result<Void> deletePaper(@Parameter(description = "试卷ID") @PathVariable Integer id) {
        paperService.deletePaper(id);
        return Result.success("试卷成功");
    }
} 