package com.back.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "排行榜评论创建参数")
public class ExamRankingCommentCreateVo {

    @Schema(description = "试卷ID，为空表示全部试卷排行榜", example = "23")
    private Integer paperId;

    @Schema(description = "评论用户ID", example = "2")
    private Long userId;

    @Schema(description = "评论内容", example = "这次考试最后一题很有区分度")
    private String content;
}
