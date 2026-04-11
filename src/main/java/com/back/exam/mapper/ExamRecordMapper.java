package com.back.exam.mapper;


import com.back.exam.entity.ExamRecord;
import com.back.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
    List<ExamRankingVO> getRanking(@Param("paperId") Integer paperId, @Param("limit") Integer limit);
} 