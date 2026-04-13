package com.back.exam.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.back.exam.entity.AnswerRecord;
import com.back.exam.entity.ExamRecord;
import com.back.exam.entity.Paper;
import com.back.exam.entity.Question;
import com.back.exam.mapper.AnswerRecordMapper;
import com.back.exam.mapper.ExamRecordMapper;
import com.back.exam.service.AnswerRecordService;
import com.back.exam.service.ExamService;
import com.back.exam.service.KimiAiService;
import com.back.exam.service.PaperService;
import com.back.exam.vo.StartExamVo;
import com.back.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
    public List<ExamRecord> getUserExamRecords(Long userId) {
        // 1. 查询该用户的所有考试记录，按时间倒序排序
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getUserId, userId);
        wrapper.orderByDesc(ExamRecord::getCreateTime);
        List<ExamRecord> records = list(wrapper);

        // 2. 遍历记录，补全试卷名称信息（用于前端展示考试名字）
        for (ExamRecord record : records) {
            if (record.getExamId() != null) {
                // 这里调用 paperService 获取试卷基本信息
                Paper paper = paperService.getById(record.getExamId());
                if (paper != null) {
                    record.setPaper(paper);
                }
            }
        }
        return records;
    }
    @Override
    public ExamRecord saveExam(StartExamVo startExamVo) {
        // 1.检查该考生是否存在正在进行的考试
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        // 确保匹配当前的考生和试卷
        queryWrapper.eq(ExamRecord::getUserId, startExamVo.getUserId());
        queryWrapper.eq(ExamRecord::getStatus, "进行中");
        queryWrapper.eq(ExamRecord::getExamId, startExamVo.getPaperId());
        ExamRecord examRecord = getOne(queryWrapper);
        if (examRecord != null) {
            return examRecord; //有对应考试的记录，返回记录即可
        }

        // 2.补全考试记录对象的属性
        examRecord = new ExamRecord();

        // 【关键修复】：确保将用户ID正确保存。通过 String.valueOf() 和 Long.valueOf() 转换，避免 Integer/Long 不匹配的问题。
        if (startExamVo.getUserId() != null) {
            examRecord.setUserId(Long.valueOf(String.valueOf(startExamVo.getUserId())));
        }

        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setStatus("进行中");
        examRecord.setWindowSwitches(0);

        // 3.进行考试记录对象保存
        save(examRecord);

        // 4.返回对应的考试记录
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
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
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
        //1.中间表保存问题
        if (!ObjectUtils.isEmpty(answers)) {
            // 【关键修复 1】：先删除当前这份考卷之前的答题记录，防止多次保存产生大量重复脏数据！
            LambdaQueryWrapper<AnswerRecord> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(AnswerRecord::getExamRecordId, examRecordId);
            answerRecordService.remove(deleteWrapper);

            // 然后再全量插入最新的答题记录
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
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }

        int correctNumber = 0 ;
        int totalScore = 0;

//        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));

        // 【关键修复 2】：过滤掉 null 数据，并防止重复的题目 ID 导致系统崩溃
        Map<Long, Question> questionMap = paper.getQuestions().stream()
                .filter(q -> q != null && q.getId() != null) // 过滤掉无效的脏数据
                .collect(Collectors.toMap(
                        Question::getId,
                        q -> q,
                        (existing, replacement) -> existing // 如果有重复的题目ID，保留第一个，忽略后面的
                ));

        for (AnswerRecord answerRecord : answerRecords) {
            try {
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                String systemAnswer = question.getAnswer().getAnswer();
                String userAnswer = answerRecord.getUserAnswer();
                if ("JUDGE".equalsIgnoreCase(question.getType())){
                    userAnswer = normalizeJudgeAnswer(userAnswer);
                }
                if (!"TEXT".equals(question.getType())) {
                    if (systemAnswer.equalsIgnoreCase(userAnswer)){
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                    }else{
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                    }
                } else {
                    String prompt = kimiAiService.buildGradingPrompt(question, userAnswer, question.getPaperScore().intValue());
                    String result = kimiAiService.callKimiAI(prompt);
                    JSONObject jsonObject = JSONObject.parseObject(result);

                    Integer aiScore = jsonObject.getInteger("score");
                    if (aiScore > question.getPaperScore().intValue()){
                        answerRecord.setIsCorrect(1);
                        answerRecord.setScore(question.getPaperScore().intValue());
                        answerRecord.setAiCorrection(jsonObject.getString("feedback"));
                    }else if (aiScore <= 0){
                        answerRecord.setIsCorrect(0);
                        answerRecord.setScore(0);
                        answerRecord.setAiCorrection(jsonObject.getString("reason"));
                    }else {
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
            totalScore += answerRecord.getScore();
            if (answerRecord.getIsCorrect() == 1){
                correctNumber++;
            }
        }
        answerRecordService.updateBatchById(answerRecords);

        // ========== 构建详细答题数据并生成AI总评 ==========

        // 1. 准备详细答题数据
        List<KimiAiService.ExamQuestionDetail> examDetails = new ArrayList<>();

        for (int i = 0; i < answerRecords.size(); i++) {
            AnswerRecord answerRecord = answerRecords.get(i);
            Question question = questionMap.get(answerRecord.getQuestionId().longValue());

            if (question == null) continue;

            KimiAiService.ExamQuestionDetail detail = new KimiAiService.ExamQuestionDetail();
            detail.setNumber(i + 1);  // 题号从1开始
            detail.setTitle(question.getTitle());
            detail.setType(question.getType());
            detail.setCategoryName(question.getCategory() != null ?
                                   question.getCategory().getName() : "未分类");
            detail.setDifficulty(question.getDifficulty());
            detail.setScore(question.getPaperScore() != null ?
                            question.getPaperScore().intValue() : 0);
            detail.setUserAnswer(answerRecord.getUserAnswer());
            detail.setCorrectAnswer(question.getAnswer() != null ?
                                    question.getAnswer().getAnswer() : "");
            detail.setIsCorrect(answerRecord.getIsCorrect());
            detail.setObtainedScore(answerRecord.getScore());
            detail.setAiCorrection(answerRecord.getAiCorrection());

            examDetails.add(detail);
        }

        // 2. 调用新的详细总评方法
        String summaryPrompt = kimiAiService.buildDetailedSummaryPrompt(
                totalScore,
                paper.getTotalScore().intValue(),
                paper.getQuestionCount(),
                correctNumber,
                examDetails
        );

        // ================== 核心修复位置 ==================
        // 给大模型调用穿上防弹衣，防止 429 限流导致交卷彻底失败
        String summary = "【系统提示】：当前 AI 访问人数过多（API限流），本次考试未能生成 AI 综合评价，但不影响您的客观题成绩与交卷。";
        try {
            // 如果 API 抽风或限流，这里会抛出异常
            summary = kimiAiService.callKimiAI(summaryPrompt);
        } catch (Exception e) {
            // 异常被拦截，记录日志，但程序继续往下走，保证交卷成功！
            log.error("调用 Kimi AI 生成试卷总评时发生异常（降级处理，保证正常交卷）: ", e);
        }
        // ==================================================

        // 3. 无论 AI 成功还是失败，下面这三行必须被安全执行，试卷才能成功提交！
        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        return examRecord;
    }

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