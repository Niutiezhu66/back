package com.atguigu.exam.mapper;


import com.atguigu.exam.entity.Question;
import com.atguigu.exam.vo.QuestionQueryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 题目Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
public interface QuestionMapper extends BaseMapper<Question> {

    //查询对应类别的题目数量，返回 List< Map< category_id, count > >
    @Select("select category_id, count(*) count from questions where is_deleted = 0 GROUP BY category_id;")
    List<Map<Long, Long >> selectCategoryQuestionCount();

    //分页查询题目列表
    IPage<Question> selectQuestionPage(IPage<Question> page, @Param("queryVo") QuestionQueryVo queryVo);

    List<Question> selectQuestionListByPaperId(Integer id);
}