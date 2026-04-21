package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.ExamRankingComment;
import com.back.exam.service.ExamRankingCommentService;
import com.back.exam.vo.ExamRankingCommentCreateVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/exam-ranking-comments")
@Tag(name = "排行榜评论", description = "排行榜评论的查询、发布与删除")
public class ExamRankingCommentController {

    @Autowired
    private ExamRankingCommentService examRankingCommentService;

    @GetMapping
    @Operation(summary = "获取排行榜评论", description = "按试卷筛选排行榜评论，不传paperId则返回全部试卷排行榜评论")
    public Result<List<ExamRankingComment>> listComments(
            @Parameter(description = "试卷ID，可选") @RequestParam(required = false) Integer paperId) {
        return Result.success(examRankingCommentService.listComments(paperId));
    }

    @PostMapping
    @Operation(summary = "发表评论", description = "登录用户在排行榜页面发布评论")
    public Result<ExamRankingComment> addComment(@RequestBody ExamRankingCommentCreateVo createVo) {
        return Result.success(examRankingCommentService.addComment(createVo), "评论发表成功");
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "用户删除自己的评论")
    public Result<String> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        examRankingCommentService.deleteComment(commentId, userId);
        return Result.success("删除评论成功");
    }
}