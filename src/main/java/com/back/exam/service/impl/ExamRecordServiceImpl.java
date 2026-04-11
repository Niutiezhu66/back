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

/**
 * 考试记录Service实现类
 * 实现考试记录相关的业务逻辑
 */
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {

    @Autowired
    private  PaperService paperService;
    @Autowired
    private AnswerRecordService answerRecordService;
    @Autowired
    private ExamRecordMapper examRecordMapper;


    @Override
    public void pageExamRecords(Page<ExamRecord> examRecordPage, String studentName, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<ExamRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(studentName), ExamRecord::getStudentName, studentName);
        if (status != null) {
            String strStatus = switch (status) {
                case 0 -> "进行中";
                case 1 -> "已完成";
                case 2 -> "已批阅";
                default -> null;
            };
            lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(strStatus),ExamRecord::getStatus,strStatus);
        }
        lambdaQueryWrapper.ge(!ObjectUtils.isEmpty(startDate),ExamRecord::getStartTime,startDate);
        lambdaQueryWrapper.le(!ObjectUtils.isEmpty(endDate),ExamRecord::getStartTime,endDate);

        page(examRecordPage,lambdaQueryWrapper);

        if (ObjectUtils.isEmpty(examRecordPage.getRecords())) return;

        List<Integer> paperIds = examRecordPage.getRecords().stream().map(ExamRecord::getExamId).toList();
        List<Paper> papers = paperService.listByIds(paperIds);

        Map<Long, Paper> paperMap = papers.stream().collect(Collectors.toMap(Paper::getId, p -> p));

        examRecordPage.getRecords().forEach(e-> e.setPaper(paperMap.get(e.getExamId().longValue())));
    }

    @Override
    public void RemoveExamRecordById(Integer id) {
        //重要的关联数据校验，有删除失败！
        //判断自身状态，进行中不能删除
        ExamRecord examRecord = getById(id);
        if ("进行中".equals(examRecord.getStatus())){
            throw new RuntimeException("正在考试中，无法直接删除！");
        }
        //删除自身数据，同时删除答题记录
        removeById(id);
        LambdaQueryWrapper<AnswerRecord> wrapper = new LambdaQueryWrapper<AnswerRecord>();
        wrapper.eq(AnswerRecord::getExamRecordId, id);
        answerRecordService.remove(wrapper);
    }

    @Override
    public List<ExamRankingVO> getRanking(Integer paperId, Integer limit) {
        return examRecordMapper.getRanking(paperId,limit);
    }
}