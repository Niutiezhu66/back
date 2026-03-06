package com.atguigu.exam.service;

import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {
    //开始考试
    ExamRecord saveExam(StartExamVo startExamVo);

    //查询考试记录详情
    ExamRecord getExamRecordDetail(Integer id);

    void submitExam(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;

    ExamRecord gradeExam(Integer examRecordId) throws InterruptedException;
}
 