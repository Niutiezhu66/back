package com.back.exam.mapper;

import com.back.exam.entity.AnswerRecord;
import com.back.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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

    @Select("SELECT COALESCE(SUM(CASE WHEN ar.is_correct = 1 THEN 1 ELSE 0 END), 0) AS numerator, " +
            "COALESCE(COUNT(ar.id), 0) AS denominator " +
            "FROM answer_record ar " +
            "WHERE ar.question_id = #{questionId} AND ar.is_deleted = 0 AND ar.create_time >= #{startTime} AND ar.create_time < #{endTime}")
    List<Map<String, Object>> selectQuestionCorrectRateStats(@Param("questionId") Long questionId,
                                                             @Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    @Select("SELECT q.*, c.name AS category_name " +
            "FROM questions q " +
            "LEFT JOIN categories c ON q.category_id = c.id AND c.is_deleted = 0 " +
            "LEFT JOIN answer_record ar ON ar.question_id = q.id AND ar.is_deleted = 0 AND ar.create_time >= #{startTime} AND ar.create_time < #{endTime} " +
            "WHERE q.is_deleted = 0 " +
            "GROUP BY q.id, c.name " +
            "ORDER BY COUNT(ar.id) DESC, q.create_time DESC " +
            "LIMIT #{size}")
    List<Question> selectWeeklyPopularFallbackQuestions(@Param("size") Integer size,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(c.name, '未分类') AS categoryName, " +
            "COALESCE(SUM(ar.score), 0) AS obtainedScore, " +
            "COALESCE(SUM(q.score), 0) AS totalScore, " +
            "COUNT(ar.id) AS answerCount " +
            "FROM answer_record ar " +
            "JOIN exam_records er ON ar.exam_record_id = er.id AND er.is_deleted = 0 " +
            "JOIN paper p ON er.exam_id = p.id AND p.is_deleted = 0 " +
            "JOIN questions q ON ar.question_id = q.id AND q.is_deleted = 0 " +
            "LEFT JOIN categories c ON q.category_id = c.id AND c.is_deleted = 0 " +
            "WHERE ar.is_deleted = 0 AND er.status = '已批阅' AND p.teacher_id = #{teacherId} " +
            "GROUP BY c.id, c.name " +
            "HAVING COALESCE(SUM(q.score), 0) > 0 " +
            "ORDER BY (1 - COALESCE(SUM(ar.score), 0) * 1.0 / COALESCE(SUM(q.score), 0)) DESC, COUNT(ar.id) DESC " +
            "LIMIT 5")
    List<Map<String, Object>> getTeacherWeakPoints(@Param("teacherId") Long teacherId);
}