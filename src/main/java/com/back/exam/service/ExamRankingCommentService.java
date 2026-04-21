package com.back.exam.service;

import com.back.exam.entity.ExamRankingComment;
import com.back.exam.vo.ExamRankingCommentCreateVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ExamRankingCommentService extends IService<ExamRankingComment> {

    List<ExamRankingComment> listComments(Integer paperId);

    ExamRankingComment addComment(ExamRankingCommentCreateVo createVo);

    void deleteComment(Long commentId, Long userId);
}
