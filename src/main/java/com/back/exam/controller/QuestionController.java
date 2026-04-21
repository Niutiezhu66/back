package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.Question;
import com.back.exam.service.QuestionService;
import com.back.exam.vo.QuestionQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
@Tag(name = "题目管理", description = "题目相关的增删改查操作，包括分页查询、随机获取、热门推荐等功能")  // Swagger标签，用于分组显示API
public class QuestionController {
    @Autowired
    private QuestionService questionService;

    @GetMapping("/list")
    @Operation(summary = "分页查询题目列表", description = "支持按分类、难度、题型、关键词进行多条件筛选的分页查询")  // Swagger接口描述
    public Result<Page<Question>> getQuestionList(
            @Parameter(description = "当前页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer page,  // 参数描述
            @Parameter(description = "每页显示数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
            QuestionQueryVo questionQueryVo ) {

        Page<Question> questionPage = new Page<>(page, size);
        questionService.queryQuestionListByPage(questionPage,questionQueryVo);
        return Result.success(questionPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询题目详情", description = "获取指定ID的题目完整信息，包括题目内容、选项、答案等详细数据")  // API描述
    public Result<Question> getQuestionById(@Parameter(description = "题目ID", example = "1") @PathVariable Long id) {
        return questionService.queryQuestionById(id);
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "增加题目热度", description = "记录一次题目浏览热度，用于首页热门推荐展示")
    public Result<String> incrementQuestionView(@Parameter(description = "题目ID", example = "1") @PathVariable Long id) {
        return questionService.incrementQuestionView(id);
    }

    @PostMapping
    @Operation(summary = "创建新题目", description = "添加新的考试题目，支持选择题、判断题、简答题等多种题型")  // API描述
    public Result<Question> createQuestion(@RequestBody Question question) {
        return questionService.saveQuestion(question);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目信息", description = "修改指定题目的内容、选项、答案等信息")  // API描述
    public Result<Question> updateQuestion(
            @Parameter(description = "题目ID") @PathVariable Long id, 
            @RequestBody Question question) {
        return questionService.updateQuestion(question);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目", description = "根据ID删除指定的题目，包括关联的选项和答案数据")  // API描述
    public Result<String> deleteQuestion(
            @Parameter(description = "题目ID") @PathVariable Long id) {
        // 根据操作结果返回不同的响应
        return questionService.deleteQuestion(id);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类查询题目", description = "获取指定分类下的所有题目列表")  // API描述
    public Result<List<Question>> getQuestionsByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {

        return Result.success(null);
    }

    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "按难度查询题目", description = "获取指定难度等级的题目列表")  // API描述
    public Result<List<Question>> getQuestionsByDifficulty(
            @Parameter(description = "难度等级，可选值：EASY(简单)/MEDIUM(中等)/HARD(困难)") @PathVariable String difficulty) {
        return Result.success(null);
    }

    @GetMapping("/random")
    @Operation(summary = "随机获取题目", description = "按指定条件随机抽取题目，用于智能组卷功能")  // API描述
    public Result<List<Question>> getRandomQuestions(
            @Parameter(description = "需要获取的题目数量", example = "10") @RequestParam(defaultValue = "10") Integer count,
            @Parameter(description = "分类ID限制条件，可选") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "难度限制条件，可选值：EASY/MEDIUM/HARD") @RequestParam(required = false) String difficulty) {

        return Result.success(null);
    }


    @GetMapping("/popular")
    @Operation(summary = "获取热门题目", description = "获取访问次数最多的热门题目，用于首页推荐展示")  // API描述
    public Result<List<Question>> getPopularQuestions(
            @Parameter(description = "返回题目数量", example = "6") @RequestParam(defaultValue = "6") Integer size) {
        return questionService.getPopularQuestion(size);
    }

    @PostMapping("/popular/refresh")
    @Operation(summary = "刷新热门题目缓存", description = "管理员功能，重置或初始化热门题目的访问计数")
    public Result<Integer> refreshPopularQuestions() {

        return Result.error("刷新热门题目缓存失败");
    }

} 