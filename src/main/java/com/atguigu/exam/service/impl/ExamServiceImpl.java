package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.AnswerRecordMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.ExamService;
import com.atguigu.exam.service.KimiAiService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {
    @Autowired
    private PaperService paperService;
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    @Autowired
    private AnswerRecordService answerRecordService;
    @Autowired
    private KimiAiService kimiAiService;



    @Override
    public ExamRecord saveExam(StartExamVo startExamVo) {
        //1.检查该考生是否存在正在进行的考试
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getStudentName, startExamVo.getStudentName());
        queryWrapper.eq(ExamRecord::getStatus, "进行中");
        queryWrapper.eq(ExamRecord::getExamId,startExamVo.getPaperId());
        ExamRecord examRecord = getOne(queryWrapper);
        if (examRecord != null) {
            return examRecord; //有对应考试的记录，返回记录即可
        }

        //2。补全考试记录对象的属性
        examRecord = new ExamRecord();
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setStatus("进行中");
        examRecord.setWindowSwitches(0);

        //3。进行考试记录对象保存
        save(examRecord);

        //4.返回对应的考试记录
        return examRecord;
    }

    @Override
    public ExamRecord getExamRecordDetail(Integer id) {
        //1. 获取考试记录详情
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("开始考试的记录已经被删除！");
        }
        //2. 获取考试记录对应试卷对象详情 【试卷 题目 选项 和 答案】
        Paper paper = paperService.getPaperById(examRecord.getExamId());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除！获取考试记录详情失败！");
        }
        examRecord.setPaper(paper);

        //3. 获取考试记录对应的答题记录集合
        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId,id);
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(lambdaQueryWrapper);

        if (!ObjectUtils.isEmpty(answerRecords)){
            //[8,2,1,3,7,4] -> 题目id
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            //[{questionId:1} -> 2 ,{questionId:2} -> 1 ,{questionId:3} -> 3,{questionId:4} ->5,{questionId:7} -> 4,{questionId:8} -> 0]
            answerRecords.sort((o1, o2) -> {
                int x = questionIdList.indexOf(o1.getQuestionId());
                int y = questionIdList.indexOf(o2.getQuestionId());
                return Integer.compare(o1.getQuestionId(),o2.getQuestionId());
            });
        }
        //4. 数据组装即可
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }

    @Override
    public void submitExam(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {
        //宏观： 提交答案中间表保存  修改考试记录数据（已完成 ，结束时间）  触发开始判卷（examRecordId）
        //1.中间表保存问题
        if (!ObjectUtils.isEmpty(answers)) {
            List<AnswerRecord> answerRecordList = answers.stream().map(vo -> new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer()))
                    .collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        //2. 暂时修改下考试记录状态（状态 -》 已完成 || 结束时间 - 设置）
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        updateById(examRecord);

        //3.调用判卷的接口
        gradeExam(examRecordId);
    }

    @Override
    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
        //1.获取考试记录和相关的信息（试卷和答题记录）
        ExamRecord examRecord = getExamRecordDetail(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null){
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
            updateById(examRecord);
            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)){
            //没有提交
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }

        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3. 修改每个答题记录的状态（得分，是否正确 0 1 2 ，text-》ai评语））
        int correctNumber = 0 ; //正确题目数量
        int totalScore = 0; //总得分

        //报错继续！ 某个记录错了，后续还需要继续判卷
        //将正确题目转成map,方便每次判断获取正确答案
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));

        for (AnswerRecord answerRecord : answerRecords) {
            try {
                //1.先获取 答题记录对应的题目对象
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                String systemAnswer = question.getAnswer().getAnswer();
                String userAnswer = answerRecord.getUserAnswer();
                if ("JUDGE".equalsIgnoreCase(question.getType())){
                    //true false
                    userAnswer = normalizeJudgeAnswer(userAnswer);
                }
                if (!"TEXT".equals(question.getType())) {
                    //2.判断题目类型(选择和判断直接判卷)
                    if (systemAnswer.equalsIgnoreCase(userAnswer)){
                        answerRecord.setIsCorrect(1); //正确
                        answerRecord.setScore(question.getPaperScore().intValue());
                    }else{
                        answerRecord.setIsCorrect(0); //正确
                        answerRecord.setScore(0);
                    }
                } else {
                    //3.简答题进行ai判断
                    //简答题
                    String prompt = kimiAiService.buildGradingPrompt(question, userAnswer, question.getPaperScore().intValue());
                    String result = kimiAiService.callKimiAI(prompt);
                    JSONObject jsonObject = JSONObject.parseObject(result);

                    Integer aiScore = jsonObject.getInteger("score");
                    if (aiScore > question.getPaperScore().intValue()){
                        //完全正确
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                        answerRecord.setAiCorrection(jsonObject.getString("feedback"));
                    }else if (aiScore <= 0){
                        //完全错误
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                        answerRecord.setAiCorrection(jsonObject.getString("reason"));
                    }else {
                        //部分正确
                        answerRecord.setIsCorrect(2);
                        answerRecord.setScore(aiScore);
                        answerRecord.setAiCorrection(jsonObject.getString("reason"));
                    }
                }
            } catch (Exception e) {
                answerRecord.setScore(0);
                answerRecord.setIsCorrect(0);
                answerRecord.setAiCorrection("判题过程出错！");
            }
            //进行记录修改
            //进行总分数赋值
            totalScore += answerRecord.getScore();
            //正确题目数量累加
            if (answerRecord.getIsCorrect() == 1){
                correctNumber++;
            }
        }
        //修改每一条学生的答题记录
        answerRecordService.updateBatchById(answerRecords);

        //进行ai生成评价，进行考试记录修改和完善
        String summaryPrompt = kimiAiService.buildSummaryPrompt(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);
        String summary = kimiAiService.callKimiAI(summaryPrompt);
        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        return examRecord;
    }

    /**
     * 标准化判断题答案，将T/F转换为TRUE/FALSE
     * @param answer 原始答案
     * @return 标准化后的答案
     */
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        return switch (normalized) {
            case "T", "TRUE", "正确" -> "TRUE";
            case "F", "FALSE", "错" -> "FALSE";
            default -> normalized;
        };
    }


}