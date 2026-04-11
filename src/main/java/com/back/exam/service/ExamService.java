package com.back.exam.service;

import com.back.exam.entity.ExamRecord;
import com.back.exam.vo.StartExamVo;
import com.back.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface ExamService extends IService<ExamRecord> {

    ExamRecord saveExam(StartExamVo startExamVo);

    ExamRecord getExamRecordDetail(Integer id);

    void submitExam(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;

    ExamRecord gradeExam(Integer examRecordId) throws InterruptedException;

    List<ExamRecord> getUserExamRecords(Long userId);
}
 