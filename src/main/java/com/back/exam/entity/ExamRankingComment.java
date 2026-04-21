package com.back.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@TableName("exam_ranking_comments")
@Data
@Schema(description = "排行榜评论")
public class ExamRankingComment extends BaseEntity {

    @Schema(description = "所属试卷ID，为空表示全部试卷排行榜评论", example = "23")
    private Integer paperId;

    @Schema(description = "评论用户ID", example = "2")
    private Long userId;

    @Schema(description = "评论用户名", example = "lzx")
    private String username;

    @Schema(description = "评论用户真实姓名", example = "张三")
    private String userRealName;

    @Schema(description = "评论内容", example = "这场考试很有挑战性")
    private String content;

    @TableField(exist = false)
    @Schema(description = "试卷名称", example = "Java测试")
    private String paperName;
}