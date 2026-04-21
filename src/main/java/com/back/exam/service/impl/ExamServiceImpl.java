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
import com.back.exam.utils.RedisUtils;
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
import java.util.Optional;
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
    @Autowired
    private RedisUtils redisUtils;

    private static final long EXAM_SUMMARY_CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;
    private static final String AI_FALLBACK_NOTICE = "【提示】以下内容为系统暂用的最近一次 AI 结果，可能不是基于本次最新数据生成。\n\n";

    @Override
    public List<ExamRecord> getUserExamRecords(Long userId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getUserId, userId);
        wrapper.orderByDesc(ExamRecord::getCreateTime);
        List<ExamRecord> records = list(wrapper);

        for (ExamRecord record : records) {
            if (record.getExamId() != null) {
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
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getUserId, startExamVo.getUserId());
        queryWrapper.eq(ExamRecord::getStatus, ExamRecord.STATUS_IN_PROGRESS);
        queryWrapper.eq(ExamRecord::getExamId, startExamVo.getPaperId());
        ExamRecord examRecord = getOne(queryWrapper);
        if (examRecord != null) {
            return examRecord;
        }

        examRecord = new ExamRecord();
        if (startExamVo.getUserId() != null) {
            examRecord.setUserId(Long.valueOf(String.valueOf(startExamVo.getUserId())));
        }
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setStatus(ExamRecord.STATUS_IN_PROGRESS);
        examRecord.setWindowSwitches(0);
        save(examRecord);
        return examRecord;
    }

    @Override
    public ExamRecord getExamRecordDetail(Integer id) {
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("开始考试的记录已经被删除！");
        }

        Paper paper = paperService.getPaperById(examRecord.getExamId());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除！获取考试记录详情失败！");
        }
        examRecord.setPaper(paper);

        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId, id);
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(lambdaQueryWrapper);

        if (!ObjectUtils.isEmpty(answerRecords)) {
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            answerRecords.sort((o1, o2) -> {
                int x = questionIdList.indexOf(o1.getQuestionId().longValue());
                int y = questionIdList.indexOf(o2.getQuestionId().longValue());
                return Integer.compare(x, y);
            });
        }
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }

    @Override
    public ExamRecord submitExam(Integer examRecordId, List<SubmitAnswerVo> answers) {
        if (!ObjectUtils.isEmpty(answers)) {
            LambdaQueryWrapper<AnswerRecord> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(AnswerRecord::getExamRecordId, examRecordId);
            answerRecordService.remove(deleteWrapper);

            List<AnswerRecord> answerRecordList = answers.stream()
                    .map(vo -> new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer()))
                    .collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }

        ExamRecord examRecord = getById(examRecordId);
        if (examRecord == null) {
            throw new RuntimeException("考试记录不存在，无法交卷！");
        }

        if (isSubmittedStatus(examRecord.getStatus())) {
            return examRecord;
        }

        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus(ExamRecord.STATUS_PENDING_GRADE);
        updateById(examRecord);
        return examRecord;
    }

    @Override
    public ExamRecord gradeExam(Integer examRecordId) {
        ExamRecord examRecord = getById(examRecordId);
        if (examRecord == null) {
            throw new RuntimeException("考试记录不存在，无法触发阅卷！");
        }
        if (ExamRecord.STATUS_GRADED.equals(examRecord.getStatus()) || ExamRecord.STATUS_GRADING.equals(examRecord.getStatus())) {
            return examRecord;
        }
        if (ExamRecord.STATUS_IN_PROGRESS.equals(examRecord.getStatus())) {
            throw new RuntimeException("考试尚未提交，无法开始阅卷！");
        }
        examRecord.setStatus(ExamRecord.STATUS_PENDING_GRADE);
        updateById(examRecord);
        return examRecord;
    }

    @Override
    public ExamRecord performGradingTask(Integer examRecordId) {
        ExamRecord currentRecord = getById(examRecordId);
        if (currentRecord == null) {
            throw new RuntimeException("考试记录不存在，无法阅卷！");
        }
        if (ExamRecord.STATUS_GRADED.equals(currentRecord.getStatus())) {
            return currentRecord;
        }

        currentRecord.setStatus(ExamRecord.STATUS_GRADING);
        updateById(currentRecord);

        try {
            return doGradeExam(examRecordId);
        } catch (Exception e) {
            log.error("考试后台阅卷失败，examRecordId={}", examRecordId, e);
            markGradeFailed(examRecordId, e.getMessage());
            throw e;
        }
    }

    private ExamRecord doGradeExam(Integer examRecordId) {
        ExamRecord examRecord = getExamRecordDetail(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null) {
            examRecord.setStatus(ExamRecord.STATUS_GRADE_FAILED);
            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
            updateById(examRecord);
            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
        }

        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)) {
            examRecord.setStatus(ExamRecord.STATUS_GRADED);
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }

        int correctNumber = 0;
        int totalScore = 0;
        Map<Long, Question> questionMap = buildQuestionMap(paper);

        for (AnswerRecord answerRecord : answerRecords) {
            try {
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                if (question == null || question.getAnswer() == null) {
                    throw new RuntimeException("题目或标准答案不存在");
                }
                totalScore += gradeSingleAnswer(answerRecord, question);
                if (answerRecord.getIsCorrect() == 1) {
                    correctNumber++;
                }
            } catch (Exception e) {
                answerRecord.setScore(0);
                answerRecord.setIsCorrect(0);
                answerRecord.setAiCorrection("判题过程出错！");
                log.warn("题目判卷失败，examRecordId={}, questionId={}, message={}", examRecordId, answerRecord.getQuestionId(), e.getMessage());
            }
        }
        answerRecordService.updateBatchById(answerRecords);

        List<KimiAiService.ExamQuestionDetail> examDetails = buildExamDetails(answerRecords, questionMap);
        String summary = buildExamSummary(examRecordId, totalScore, correctNumber, paper, examDetails);

        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus(ExamRecord.STATUS_GRADED);
        updateById(examRecord);
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }

    private Map<Long, Question> buildQuestionMap(Paper paper) {
        return paper.getQuestions().stream()
                .filter(q -> q != null && q.getId() != null)
                .collect(Collectors.toMap(
                        Question::getId,
                        q -> q,
                        (existing, replacement) -> existing
                ));
    }

    private int gradeSingleAnswer(AnswerRecord answerRecord, Question question) {
        String systemAnswer = question.getAnswer().getAnswer();
        String userAnswer = answerRecord.getUserAnswer();
        if ("JUDGE".equalsIgnoreCase(question.getType())) {
            userAnswer = normalizeJudgeAnswer(userAnswer);
        }

        if (!"TEXT".equals(question.getType())) {
            if (systemAnswer != null && systemAnswer.equalsIgnoreCase(userAnswer == null ? "" : userAnswer)) {
                answerRecord.setIsCorrect(1);
                answerRecord.setScore(question.getPaperScore().intValue());
            } else {
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
            }
            answerRecord.setAiCorrection(null);
            return answerRecord.getScore();
        }

        String prompt = kimiAiService.buildGradingPrompt(question, userAnswer == null ? "" : userAnswer, question.getPaperScore().intValue());
        String result;
        try {
            result = kimiAiService.callKimiAI(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI 阅卷任务被中断", e);
        }
        JSONObject jsonObject = JSONObject.parseObject(result);

        Integer aiScore = jsonObject.getInteger("score");
        if (aiScore == null) {
            aiScore = 0;
        }
        if (aiScore > question.getPaperScore().intValue()) {
            answerRecord.setIsCorrect(1);
            answerRecord.setScore(question.getPaperScore().intValue());
            answerRecord.setAiCorrection(jsonObject.getString("feedback"));
        } else if (aiScore <= 0) {
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(0);
            answerRecord.setAiCorrection(firstNonBlank(jsonObject.getString("reason"), jsonObject.getString("feedback"), "未作答或答案不正确"));
        } else {
            answerRecord.setIsCorrect(2);
            answerRecord.setScore(aiScore);
            answerRecord.setAiCorrection(firstNonBlank(jsonObject.getString("reason"), jsonObject.getString("feedback"), "答案部分正确"));
        }
        return answerRecord.getScore();
    }

    private List<KimiAiService.ExamQuestionDetail> buildExamDetails(List<AnswerRecord> answerRecords, Map<Long, Question> questionMap) {
        List<KimiAiService.ExamQuestionDetail> examDetails = new ArrayList<>();
        for (int i = 0; i < answerRecords.size(); i++) {
            AnswerRecord answerRecord = answerRecords.get(i);
            Question question = questionMap.get(answerRecord.getQuestionId().longValue());
            if (question == null) {
                continue;
            }

            KimiAiService.ExamQuestionDetail detail = new KimiAiService.ExamQuestionDetail();
            detail.setNumber(i + 1);
            detail.setTitle(question.getTitle());
            detail.setType(question.getType());
            detail.setCategoryName(question.getCategory() != null ? question.getCategory().getName() : "未分类");
            detail.setDifficulty(question.getDifficulty());
            detail.setScore(question.getPaperScore() != null ? question.getPaperScore().intValue() : 0);
            detail.setUserAnswer(answerRecord.getUserAnswer());
            detail.setCorrectAnswer(question.getAnswer() != null ? question.getAnswer().getAnswer() : "");
            detail.setIsCorrect(answerRecord.getIsCorrect());
            detail.setObtainedScore(answerRecord.getScore());
            detail.setAiCorrection(answerRecord.getAiCorrection());
            examDetails.add(detail);
        }
        return examDetails;
    }

    private String buildExamSummary(Integer examRecordId, int totalScore, int correctNumber, Paper paper,
                                    List<KimiAiService.ExamQuestionDetail> examDetails) {
        String summaryPrompt = kimiAiService.buildDetailedSummaryPrompt(
                totalScore,
                paper.getTotalScore().intValue(),
                paper.getQuestionCount(),
                correctNumber,
                examDetails
        );

        String summaryCacheKey = buildExamSummaryCacheKey(examRecordId);
        String cachedSummary = getCachedText(summaryCacheKey);
        String summary = "【系统提示】：当前 AI 访问人数过多（API限流），本次考试未能生成 AI 综合评价，但不影响您的客观题成绩与交卷。";
        try {
            String aiSummary = kimiAiService.callKimiAIQuickFail(summaryPrompt);
            if (aiSummary != null && !aiSummary.isBlank()) {
                summary = aiSummary;
                redisUtils.set(summaryCacheKey, aiSummary, EXAM_SUMMARY_CACHE_TTL_SECONDS);
            }
        } catch (Exception e) {
            if (cachedSummary != null) {
                summary = addFallbackNotice(cachedSummary);
            }
            log.warn("调用 Kimi AI 生成试卷总评时被降级，examRecordId={}, reason={}", examRecordId, e.getMessage());
        }
        return summary;
    }

    private void markGradeFailed(Integer examRecordId, String message) {
        ExamRecord examRecord = getById(examRecordId);
        if (examRecord == null) {
            return;
        }
        examRecord.setStatus(ExamRecord.STATUS_GRADE_FAILED);
        if (examRecord.getAnswers() == null || examRecord.getAnswers().isBlank()) {
            examRecord.setAnswers("【系统提示】：AI 阅卷暂时失败，请稍后重试。" + (message == null ? "" : "\n原因：" + message));
        }
        updateById(examRecord);
    }

    private boolean isSubmittedStatus(String status) {
        return ExamRecord.STATUS_PENDING_GRADE.equals(status)
                || ExamRecord.STATUS_GRADING.equals(status)
                || ExamRecord.STATUS_GRADED.equals(status)
                || ExamRecord.STATUS_GRADE_FAILED.equals(status)
                || ExamRecord.STATUS_COMPLETED.equals(status);
    }

    private String buildExamSummaryCacheKey(Integer examRecordId) {
        return "ai:fallback:exam-summary:record:" + examRecordId;
    }

    private String getCachedText(String key) {
        Object cachedValue = redisUtils.get(key);
        if (cachedValue == null) {
            return null;
        }
        String content = cachedValue.toString();
        return content.isBlank() ? null : content;
    }

    private String addFallbackNotice(String content) {
        return content.startsWith(AI_FALLBACK_NOTICE) ? content : AI_FALLBACK_NOTICE + content;
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
