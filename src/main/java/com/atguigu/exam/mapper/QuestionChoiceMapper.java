package com.atguigu.exam.mapper;


import com.atguigu.exam.entity.QuestionChoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {

    //根据题目id查询选项集合
    @Select("select * from question_choices where question_id = #{questionId} and is_deleted = 0 order by sort asc")
    List<QuestionChoice> selectListByQuestionId(Integer questionId);

}