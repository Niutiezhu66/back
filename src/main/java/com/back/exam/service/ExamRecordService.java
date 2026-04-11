package com.back.exam.service;

import com.back.exam.entity.ExamRecord;
import com.back.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.List;

public interface ExamRecordService extends IService<ExamRecord> {

    void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName, Integer status, String startDate, String endDate);

    void RemoveExamRecordById(Integer id);

    List<ExamRankingVO> getRanking(Integer paperId, Integer limit);
}