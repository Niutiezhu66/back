package com.back.exam.service.impl;

import com.back.exam.entity.ExamRecord;
import com.back.exam.entity.Paper;
import com.back.exam.mapper.*;
import com.back.exam.mapper.*;
import com.back.exam.service.ExamRecordService;
import com.back.exam.service.KimiAiService;
import com.back.exam.service.PaperService;
import com.back.exam.service.StatsService;
import com.back.exam.utils.RedisUtils;
import com.back.exam.vo.DiagnosisResultVo;
import com.back.exam.vo.StatsVo;
import com.back.exam.vo.TeacherStatsVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据服务实现类
 */
@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private QuestionMapper questionMapper;  // 题目Mapper
    
    @Autowired
    private UserMapper userMapper;  // 用户Mapper
    
    @Autowired
    private ExamRecordMapper examRecordMapper;  // 考试记录Mapper
    
    @Autowired
    private CategoryMapper categoryMapper;  // 分类Mapper
    
    @Autowired
    private PaperMapper paperMapper;  // 试卷Mapper

    @Override
    public StatsVo getSystemStats() {
        StatsVo stats = new StatsVo();
        
        try {
            // 统计题目总数  // 查询题目数量
            Long questionCount = questionMapper.selectCount(new QueryWrapper<>());
            stats.setQuestionCount(questionCount);
            log.info("题目总数: {}", questionCount);
        } catch (Exception e) {
            log.error("查询题目总数失败: {}", e.getMessage());
            stats.setQuestionCount(0L);
        }
        
        try {
            // 统计用户总数  // 查询用户数量
            Long userCount = userMapper.selectCount(new QueryWrapper<>());
            stats.setUserCount(userCount);
            log.info("用户总数: {}", userCount);
        } catch (Exception e) {
            log.error("查询用户总数失败: {}", e.getMessage());
            stats.setUserCount(0L);
        }
        
        try {
            // 统计考试总场次  // 查询考试记录总数
            Long examCount = examRecordMapper.selectCount(new QueryWrapper<>());
            stats.setExamCount(examCount);
            log.info("考试总场次: {}", examCount);
        } catch (Exception e) {
            log.error("查询考试总场次失败: {}", e.getMessage());
            stats.setExamCount(0L);
        }
        
        try {
            // 统计今日考试次数  // 查询今天的考试记录（使用create_time字段）
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();  // 今天00:00:00
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);  // 今天23:59:59
            
            QueryWrapper<ExamRecord> todayQueryWrapper = new QueryWrapper<>();
            todayQueryWrapper.between("create_time", startOfDay, endOfDay);  // 使用正确的字段名
            Long todayExamCount = examRecordMapper.selectCount(todayQueryWrapper);
            stats.setTodayExamCount(todayExamCount);
            log.info("今日考试次数: {}", todayExamCount);
        } catch (Exception e) {
            log.error("查询今日考试次数失败: {}", e.getMessage());
            stats.setTodayExamCount(0L);
        }
        
        try {
            // 统计分类总数  // 查询分类数量
            Long categoryCount = categoryMapper.selectCount(new QueryWrapper<>());
            stats.setCategoryCount(categoryCount);
            log.info("分类总数: {}", categoryCount);
        } catch (Exception e) {
            log.error("查询分类总数失败: {}", e.getMessage());
            stats.setCategoryCount(0L);
        }
        
        try {
            // 统计试卷总数  // 查询试卷数量
            Long paperCount = paperMapper.selectCount(new QueryWrapper<>());
            stats.setPaperCount(paperCount);
            log.info("试卷总数: {}", paperCount);
        } catch (Exception e) {
            log.error("查询试卷总数失败: {}", e.getMessage());
            stats.setPaperCount(0L);
        }
        
        log.info("系统统计数据获取完成: {}", stats);
        return stats;
    }


    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private KimiAiService kimiAiService;

    @Autowired
    private RedisUtils redisUtils;

    private static final long STUDENT_DIAGNOSIS_CACHE_TTL_SECONDS = 24 * 60 * 60;
    private static final long TEACHER_DIAGNOSIS_CACHE_TTL_SECONDS = 12 * 60 * 60;
    private static final String FALLBACK_NOTICE = "【提示】以下内容为系统暂用的最近一次 AI 结果，可能不是基于本次最新数据生成。\n\n";

    @Override
    public DiagnosisResultVo generateAIDiagnosis(Long userId) throws InterruptedException {
        DiagnosisResultVo result = new DiagnosisResultVo();

        // 1. 获取雷达图分类掌握数据
        List<Map<String, Object>> masteryList = answerRecordMapper.getStudentCategoryMastery(userId);

        List<DiagnosisResultVo.CategoryScore> radarData = new ArrayList<>();
        List<String> strongPoints = new ArrayList<>();
        List<String> weakPoints = new ArrayList<>();
        double totalPercentage = 0.0;
        int validCategories = 0;

        StringBuilder aiDataPrompt = new StringBuilder();
        aiDataPrompt.append("【各分类掌握率数据】\n");

        for (Map<String, Object> map : masteryList) {
            String categoryName = map.get("categoryName") != null ? map.get("categoryName").toString() : "未分类";
            double obtained = Double.parseDouble(map.get("obtainedScore").toString());
            double total = Double.parseDouble(map.get("totalScore").toString());

            // 计算掌握度百分比 (0-100)
            int percentage = 0;
            if (total > 0) {
                percentage = (int) ((obtained / total) * 100);
            }

            DiagnosisResultVo.CategoryScore cs = new DiagnosisResultVo.CategoryScore();
            cs.setName(categoryName);
            cs.setScore(percentage);
            radarData.add(cs);

            totalPercentage += percentage;
            validCategories++;

            aiDataPrompt.append("- ").append(categoryName).append("：").append(percentage).append("%\n");

            if (percentage >= 80) strongPoints.add(categoryName);
            else if (percentage < 60) weakPoints.add(categoryName);
        }

        // 2. 获取近期盲区（错题）
        List<Map<String, Object>> wrongList = answerRecordMapper.getRecentWrongQuestions(userId);
        List<DiagnosisResultVo.BlindSpot> blindSpots = new ArrayList<>();

        aiDataPrompt.append("\n【近期典型错题记录】\n");
        int index = 1;
        for (Map<String, Object> map : wrongList) {
            String cat = map.get("categoryName") != null ? map.get("categoryName").toString() : "通用";
            String title = map.get("questionTitle").toString();
            String uAnswer = map.get("userAnswer") != null ? map.get("userAnswer").toString() : "（未作答）";

            DiagnosisResultVo.BlindSpot bs = new DiagnosisResultVo.BlindSpot();
            bs.setTitle("薄弱知识点：" + cat);
            bs.setDescription(title);
            bs.setErrorExample(uAnswer);
            blindSpots.add(bs);

            aiDataPrompt.append(index++).append(". 属于[").append(cat).append("]的题目：").append(title).append("\n");
            aiDataPrompt.append("   该生错误作答：").append(uAnswer).append("\n");
        }

        // 处理没有考试数据的情况
        if (validCategories == 0) {
            result.setHealthScore(0);
            result.setAiSuggestion("暂无足够的考试数据供 AI 分析，请先参加几次考试后再来诊断吧！");
            return result;
        }

        // 3. 构建结果对象
        result.setHealthScore((int) (totalPercentage / validCategories));
        result.setRadarData(radarData);
        result.setStrongPoints(strongPoints.isEmpty() ? List.of("暂无明显优势") : strongPoints);
        result.setWeakPoints(weakPoints.isEmpty() ? List.of("基础较均衡") : weakPoints);
        result.setBlindSpots(blindSpots);

        // 4. 调用 Kimi AI 生成提升规划
        String prompt = "你是一位资深的AI学习导师。以下是一名学生真实的学习数据：\n\n"
                + aiDataPrompt.toString()
                + "\n\n请直接输出一段 Markdown 格式的专属学习提升规划。要求：\n"
                + "1. 语气亲切、有鼓励性。\n"
                + "2. 基于提供的【各分类掌握率数据】，简评其知识结构。\n"
                + "3. 结合提供的【近期典型错题记录】，指出其具体的认知盲区。\n"
                + "4. 给出切实可行、分步骤的复习建议。";

        String diagnosisCacheKey = buildStudentDiagnosisCacheKey(userId);
        String cachedSuggestion = getCachedText(diagnosisCacheKey);
        String aiSuggestion = "### 💡 AI 导师提示\n\n当前 AI 咨询人数过多（触发了 API 接口限流），暂时无法生成长篇学习建议文本。\n\n**不过别担心！** 系统已为您精准计算出了上方的**【真实掌握率雷达图】**以及**【核心错题盲区】**。请根据上方的雷达图短板和列出的错题进行针对性复习。稍后您可以点击“重新生成”按钮再次尝试！";
        try {
            // 尝试调用大模型
            String response = kimiAiService.callKimiAI(prompt);
            if (response != null && !response.isEmpty()) {
                String cleanedResponse = stripOuterMarkdownFence(response);
                aiSuggestion = cleanedResponse;
                redisUtils.set(diagnosisCacheKey, cleanedResponse, STUDENT_DIAGNOSIS_CACHE_TTL_SECONDS);
            }
        } catch (Exception e) {
            if (cachedSuggestion != null) {
                aiSuggestion = addFallbackNotice(stripOuterMarkdownFence(cachedSuggestion));
            }
            log.error("AI生成学情诊断时触发限制或网络异常 (不影响其他数据展示): ", e);
        }

        result.setAiSuggestion(aiSuggestion);

        return result;
    }

    @Autowired
    private PaperService paperService;
    @Autowired
    private ExamRecordService examRecordService;

    @Override
    public TeacherStatsVo generateTeacherOverview(Long teacherId) {
        TeacherStatsVo vo = new TeacherStatsVo();

        LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(Paper::getTeacherId, teacherId);
        List<Paper> papers = paperService.list(paperWrapper);

        if (papers.isEmpty()) {
            vo.setAiReport("您当前尚未发布任何试卷，暂无学情数据。");
            return vo;
        }
        vo.setTotalExams(papers.size());

        List<Long> paperIds = papers.stream().map(Paper::getId).collect(Collectors.toList());
        Map<Long, Double> paperTotalScoreMap = papers.stream().collect(Collectors.toMap(
                Paper::getId,
                paper -> paper.getTotalScore() != null ? paper.getTotalScore().doubleValue() : 0.0,
                (existing, replacement) -> existing
        ));

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExamRecord> recordWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        recordWrapper.in("exam_id", paperIds).eq("status", "已批阅");

        List<ExamRecord> records = examRecordService.list(recordWrapper);
        vo.setTotalStudents(records.size());

        if (records.isEmpty()) {
            vo.setAiReport("您的试卷暂时还没有学生作答或批阅完成，请等待学生考试后再来查看。");
            vo.setScoreDistribution(new ArrayList<>());
            vo.setWeakPoints(new ArrayList<>());
            vo.setAvgScore(0.0);
            vo.setPassRate(0.0);
            return vo;
        }

        double totalScoreSum = 0;
        int validPercentageCount = 0;
        int passCount = 0;
        int fail = 0, pass = 0, good = 0, excellent = 0;

        for (ExamRecord record : records) {
            double score = record.getScore() != null ? record.getScore().doubleValue() : 0.0;
            totalScoreSum += score;

            Double paperTotalScore = paperTotalScoreMap.get(record.getExamId() != null ? record.getExamId().longValue() : null);
            if (paperTotalScore == null || paperTotalScore <= 0) {
                continue;
            }

            double percentage = (score / paperTotalScore) * 100;
            validPercentageCount++;

            if (percentage >= 60) passCount++;

            if (percentage < 60) fail++;
            else if (percentage < 80) pass++;
            else if (percentage < 90) good++;
            else excellent++;
        }

        vo.setAvgScore(Math.round((totalScoreSum / records.size()) * 10.0) / 10.0);
        vo.setPassRate(validPercentageCount == 0 ? 0.0 : Math.round(((double) passCount / validPercentageCount) * 100.0 * 10.0) / 10.0);

        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(Map.of("name", "不及格(<60%)", "value", fail));
        distribution.add(Map.of("name", "及格(60%-79%)", "value", pass));
        distribution.add(Map.of("name", "良好(80%-89%)", "value", good));
        distribution.add(Map.of("name", "优秀(≥90%)", "value", excellent));
        vo.setScoreDistribution(distribution);

        List<Map<String, Object>> weakPointStats = answerRecordMapper.getTeacherWeakPoints(teacherId);
        List<Map<String, Object>> weakPoints = weakPointStats.stream()
                .map(item -> {
                    String categoryName = item.get("categoryName") != null ? item.get("categoryName").toString() : "未分类";
                    double obtainedScore = item.get("obtainedScore") != null ? Double.parseDouble(item.get("obtainedScore").toString()) : 0.0;
                    double totalScore = item.get("totalScore") != null ? Double.parseDouble(item.get("totalScore").toString()) : 0.0;
                    double errorRate = totalScore <= 0 ? 0.0 : (1 - (obtainedScore / totalScore)) * 100;
                    double roundedErrorRate = Math.round(Math.max(0.0, Math.min(100.0, errorRate)) * 10.0) / 10.0;
                    Map<String, Object> weakPoint = new HashMap<>();
                    weakPoint.put("name", categoryName);
                    weakPoint.put("value", roundedErrorRate);
                    return weakPoint;
                })
                .collect(Collectors.toList());
        vo.setWeakPoints(weakPoints);

        vo.setAiReport(String.format(
                "### 教学分析摘要\n\n" +
                        "- 当前共统计 **%d** 人次已批阅记录。\n" +
                        "- 班级平均分为 **%.1f** 分，整体及格率为 **%.1f%%**。\n" +
                        "- 分数结构：不及格 **%d** 人，及格 **%d** 人，良好 **%d** 人，优秀 **%d** 人。\n\n" +
                        "### 建议\n\n" +
                        "1. 优先针对得分率低于60%%与60%%-79%%区间学生做基础巩固。\n" +
                        "2. 对得分率80%%以上学生增加综合应用训练，拉开优秀层。\n" +
                        "3. 结合错题与课堂表现，安排分层复习与二次讲解。",
                vo.getTotalStudents(), vo.getAvgScore(), vo.getPassRate(), fail, pass, good, excellent
        ));
        return vo;
    }

    @Override
    public TeacherStatsVo generateTeacherDiagnosis(Long teacherId) throws InterruptedException {
        TeacherStatsVo vo = generateTeacherOverview(teacherId);
        if (vo.getTotalExams() == null || vo.getTotalExams() == 0 || vo.getTotalStudents() == null || vo.getTotalStudents() == 0) {
            return vo;
        }

        int fail = extractDistributionValue(vo.getScoreDistribution(), "不及格(<60%)");
        int pass = extractDistributionValue(vo.getScoreDistribution(), "及格(60%-79%)");
        int good = extractDistributionValue(vo.getScoreDistribution(), "良好(80%-89%)");
        int excellent = extractDistributionValue(vo.getScoreDistribution(), "优秀(≥90%)");

        String prompt = String.format(
                "你是一位资深的教研主管。以下是某位老师名下班级近期考试的总体学情数据：\n" +
                        "- 参考总人次：%d 人\n" +
                        "- 班级平均分：%.1f 分\n" +
                        "- 整体及格率：%.1f%%\n" +
                        "分数段分布：不及格%d人，及格%d人，良好%d人，优秀%d人。\n\n" +
                        "请直接输出一段 Markdown 格式的《教学分析与改进建议报告》。要求：\n" +
                        "1. 语气专业、客观，具有宏观指导性。\n" +
                        "2. 点评该班级的整体成绩结构是健康还是偏科。\n" +
                        "3. 针对不及格和中等生群体，给出2-3条非常具体的课堂改进或复习策略。",
                vo.getTotalStudents(), vo.getAvgScore(), vo.getPassRate(), fail, pass, good, excellent
        );

        String diagnosisCacheKey = buildTeacherDiagnosisCacheKey(teacherId);
        String cachedReport = getCachedText(diagnosisCacheKey);
        try {
            String aiResponse = kimiAiService.callKimiAI(prompt);
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                String cleanedResponse = stripOuterMarkdownFence(aiResponse);
                vo.setAiReport(cleanedResponse);
                redisUtils.set(diagnosisCacheKey, cleanedResponse, TEACHER_DIAGNOSIS_CACHE_TTL_SECONDS);
            }
        } catch (Exception e) {
            if (cachedReport != null) {
                vo.setAiReport(addFallbackNotice(stripOuterMarkdownFence(cachedReport)));
            }
            log.warn("教师端AI报告生成失败，已降级为本地分析：{}", e.getMessage());
        }
        return vo;
    }

    private String buildStudentDiagnosisCacheKey(Long userId) {
        return "ai:fallback:student-diagnosis:user:" + userId;
    }

    private String buildTeacherDiagnosisCacheKey(Long teacherId) {
        return "ai:fallback:teacher-diagnosis:teacher:" + teacherId;
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
        return content.startsWith(FALLBACK_NOTICE) ? content : FALLBACK_NOTICE + content;
    }

    private String stripOuterMarkdownFence(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String normalized = content.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);

        int start = 0;
        while (start < lines.length && lines[start].isBlank()) {
            start++;
        }

        int end = lines.length - 1;
        while (end >= start && lines[end].isBlank()) {
            end--;
        }

        if (start >= end) {
            return content;
        }

        String firstLine = lines[start].trim().toLowerCase(Locale.ROOT);
        String lastLine = lines[end].trim();
        boolean hasOpeningFence = firstLine.equals("```") || firstLine.equals("```markdown") || firstLine.equals("```md");
        if (!hasOpeningFence || !"```".equals(lastLine)) {
            return content;
        }

        List<String> cleanedLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (i == start || i == end) {
                continue;
            }
            cleanedLines.add(lines[i]);
        }

        return String.join("\n", cleanedLines).trim();
    }

    private int extractDistributionValue(List<Map<String, Object>> distribution, String name) {
        if (distribution == null) {
            return 0;
        }
        return distribution.stream()
                .filter(item -> name.equals(item.get("name")))
                .map(item -> item.get("value"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElse(0);
    }

} 