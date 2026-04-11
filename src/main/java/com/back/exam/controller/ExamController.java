package com.back.exam.controller;


import com.back.exam.common.Result;
import com.back.exam.entity.ExamRecord;
import com.back.exam.service.ExamService;
import com.back.exam.vo.StartExamVo;
import com.back.exam.vo.SubmitAnswerVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins = "*")
@Tag(name = "考试管理", description = "考试流程相关操作，包括开始考试、答题提交、AI批阅、成绩查询等功能")  // Swagger API分组
public class ExamController {
    @Autowired
    private ExamService examService;


    @PostMapping("/start")  
    @Operation(summary = "开始考试", description = "学生开始考试，创建考试记录并返回试卷内容")  // API描述
    public Result<ExamRecord> startExam(@RequestBody StartExamVo startExamVo) {
        // ✨ 新增：安全校验，确保前端传了用户ID
        if (startExamVo.getUserId() == null) {
            return Result.error("获取考生的用户状态失败，请重新登录！");
        }
        ExamRecord examRecord = examService.saveExam(startExamVo);
        return Result.success(examRecord, "考试开始成功");
    }

  
    @PostMapping("/{examRecordId}/submit")  
    @Operation(summary = "提交考试答案", description = "学生提交考试答案，系统记录答题情况")  // API描述
    public Result<Void> submitAnswers(
            @Parameter(description = "考试记录ID") @PathVariable Integer examRecordId, 
            @RequestBody List<SubmitAnswerVo> answers) throws InterruptedException {
        examService.submitExam(examRecordId,answers);
        return Result.success("答案提交成功");
    }

  
    @PostMapping("/{examRecordId}/grade")  
    @Operation(summary = "AI自动批阅", description = "使用AI技术自动批阅试卷，特别是简答题的智能评分")  // API描述
    public Result<ExamRecord> gradeExam(
            @Parameter(description = "考试记录ID") @PathVariable Integer examRecordId) throws InterruptedException {
        ExamRecord examRecord = examService.gradeExam(examRecordId);
        return Result.success(examRecord, "试卷批阅完成");
    }

 
    @GetMapping("/{id}") 
    @Operation(summary = "查询考试记录详情", description = "获取指定考试记录的详细信息，包括答题情况和得分")  // API描述
    public Result<ExamRecord> getExamRecordById(
            @Parameter(description = "考试记录ID") @PathVariable Integer id) {
        ExamRecord examRecord = examService.getExamRecordDetail(id);
        return Result.success(examRecord);
    }


    /**
     * 获取考试记录列表 - 查询个人的考试记录
     */
    @GetMapping("/records")
    @Operation(summary = "获取考试记录列表", description = "获取个人的考试记录列表，包含基本信息和成绩")
    public Result<List<ExamRecord>> getMyRecords(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            return Result.error("未获取到当前用户信息，无法查询考试记录");
        }
        List<ExamRecord> records = examService.getUserExamRecords(userId);
        return Result.success(records);
    }


} 