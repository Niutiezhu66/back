package com.back.exam.mapper;

import com.back.exam.entity.AnswerRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {

    // 1. 统计该学生在各个知识点分类下的得分情况
    @Select("SELECT c.name as categoryName, " +
            "SUM(ar.score) as obtainedScore, " +
            "SUM(q.score) as totalScore " +
            "FROM answer_record ar " +
            "JOIN questions q ON ar.question_id = q.id " +
            "LEFT JOIN categories c ON q.category_id = c.id " +
            "JOIN exam_records er ON ar.exam_record_id = er.id " +
            "WHERE er.user_id = #{userId} AND ar.is_deleted = 0 AND er.status = '已批阅' " +
            "GROUP BY c.id, c.name")
    List<Map<String, Object>> getStudentCategoryMastery(@Param("userId") Long userId);

    // 2. 获取该学生最近答错的 3 道典型错题
    @Select("SELECT q.title as questionTitle, c.name as categoryName, ar.user_answer as userAnswer " +
            "FROM answer_record ar " +
            "JOIN questions q ON ar.question_id = q.id " +
            "LEFT JOIN categories c ON q.category_id = c.id " +
            "JOIN exam_records er ON ar.exam_record_id = er.id " +
            "WHERE er.user_id = #{userId} AND ar.is_correct = 0 AND ar.is_deleted = 0 AND er.status = '已批阅' " +
            "ORDER BY ar.create_time DESC LIMIT 3")
    List<Map<String, Object>> getRecentWrongQuestions(@Param("userId") Long userId);
}