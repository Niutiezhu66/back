package com.back.exam.service.impl;

import com.back.exam.entity.ExamRankingComment;
import com.back.exam.entity.Paper;
import com.back.exam.entity.User;
import com.back.exam.mapper.ExamRankingCommentMapper;
import com.back.exam.service.ExamRankingCommentService;
import com.back.exam.service.PaperService;
import com.back.exam.service.UserService;
import com.back.exam.vo.ExamRankingCommentCreateVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ExamRankingCommentServiceImpl extends ServiceImpl<ExamRankingCommentMapper, ExamRankingComment>
        implements ExamRankingCommentService {

    @Autowired
    private UserService userService;

    @Autowired
    private PaperService paperService;

    @Override
    public List<ExamRankingComment> listComments(Integer paperId) {
        if (paperId != null) {
            Paper paper = paperService.getById(paperId);
            if (paper == null) {
                throw new RuntimeException("试卷不存在");
            }
        }

        LambdaQueryWrapper<ExamRankingComment> wrapper = new LambdaQueryWrapper<>();
        if (paperId == null) {
            wrapper.isNull(ExamRankingComment::getPaperId);
        } else {
            wrapper.eq(ExamRankingComment::getPaperId, paperId);
        }
        wrapper.orderByDesc(ExamRankingComment::getCreateTime);
        return list(wrapper);
    }

    @Override
    public ExamRankingComment addComment(ExamRankingCommentCreateVo createVo) {
        if (createVo.getUserId() == null) {
            throw new RuntimeException("请先登录后再评论");
        }
        if (!StringUtils.hasText(createVo.getContent())) {
            throw new RuntimeException("评论内容不能为空");
        }

        String content = createVo.getContent().trim();
        if (content.length() > 500) {
            throw new RuntimeException("评论内容不能超过500字");
        }

//        User user = userService.getById(createVo.getUserId());
        User user = userService.lambdaQuery()
                .eq(User::getUserId, createVo.getUserId())
                .one();

        if (user == null) {
            throw new RuntimeException("评论用户不存在");
        }

        if (createVo.getPaperId() != null) {
            Paper paper = paperService.getById(createVo.getPaperId());
            if (paper == null) {
                throw new RuntimeException("试卷不存在");
            }
        }

        ExamRankingComment comment = new ExamRankingComment();
        comment.setPaperId(createVo.getPaperId());
        comment.setUserId(Long.valueOf(user.getUserId()));
        comment.setUsername(user.getUsername());
        comment.setUserRealName(user.getRealName());
        comment.setContent(content);
        save(comment);
        return getById(comment.getId());
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        if (commentId == null) {
            throw new RuntimeException("评论ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("请先登录后再删除评论");
        }

        ExamRankingComment comment = getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        if (!userId.equals(comment.getUserId())) {
            throw new RuntimeException("只能删除自己的评论");
        }

        removeById(commentId);
    }
}
