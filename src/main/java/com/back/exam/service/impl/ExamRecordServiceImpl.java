package com.back.exam.service.impl;

import com.back.exam.entity.AnswerRecord;
import com.back.exam.entity.ExamRecord;
import com.back.exam.entity.Paper;
import com.back.exam.mapper.ExamRecordMapper;
import com.back.exam.service.AnswerRecordService;
import com.back.exam.service.ExamRecordService;
import com.back.exam.service.PaperService;
import com.back.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {

    @Autowired
    private PaperService paperService;
    @Autowired
    private AnswerRecordService answerRecordService;
    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Override
    public void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName, String studentNumber, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<ExamRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(studentName), ExamRecord::getStudentName, studentName);
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(studentNumber), ExamRecord::getUserId, studentNumber);
        if (status != null) {
            switch (status) {
                case 0 -> lambdaQueryWrapper.eq(ExamRecord::getStatus, ExamRecord.STATUS_IN_PROGRESS);
                case 1 -> lambdaQueryWrapper.in(ExamRecord::getStatus,
                        ExamRecord.STATUS_PENDING_GRADE,
                        ExamRecord.STATUS_GRADING,
                        ExamRecord.STATUS_GRADE_FAILED,
                        ExamRecord.STATUS_COMPLETED);
                case 2 -> lambdaQueryWrapper.eq(ExamRecord::getStatus, ExamRecord.STATUS_GRADED);
                default -> {
                }
            }
        }
        lambdaQueryWrapper.ge(!ObjectUtils.isEmpty(startDate), ExamRecord::getStartTime, startDate);
        lambdaQueryWrapper.le(!ObjectUtils.isEmpty(endDate), ExamRecord::getStartTime, endDate);

        page(examRecordPage, lambdaQueryWrapper);

        if (ObjectUtils.isEmpty(examRecordPage.getRecords())) {
            return;
        }

        List<Integer> paperIds = examRecordPage.getRecords().stream().map(ExamRecord::getExamId).toList();
        List<Paper> papers = paperService.listByIds(paperIds);
        Map<Long, Paper> paperMap = papers.stream().collect(Collectors.toMap(Paper::getId, p -> p));

        examRecordPage.getRecords().forEach(e -> {
            e.setPaper(paperMap.get(e.getExamId().longValue()));
            e.setStudentNumber(e.getUserId() == null ? "" : String.valueOf(e.getUserId()));
        });
    }

    @Override
    public void RemoveExamRecordById(Integer id) {
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("考试记录不存在或已被删除！");
        }
        if (ExamRecord.STATUS_IN_PROGRESS.equals(examRecord.getStatus())) {
            throw new RuntimeException("正在考试中，无法直接删除！");
        }
        removeById(id);
        LambdaQueryWrapper<AnswerRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerRecord::getExamRecordId, id);
        answerRecordService.remove(wrapper);
    }

    @Override
    public List<ExamRankingVO> getRanking(Integer paperId, Integer limit) {
        return examRecordMapper.getRanking(paperId, limit);
    }
}
