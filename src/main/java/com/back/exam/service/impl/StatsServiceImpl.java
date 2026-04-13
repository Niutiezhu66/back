package com.back.exam.service.impl;

import com.back.exam.entity.ExamRecord;
import com.back.exam.mapper.*;
import com.back.exam.mapper.*;
import com.back.exam.service.KimiAiService;
import com.back.exam.service.StatsService;
import com.back.exam.vo.DiagnosisResultVo;
import com.back.exam.vo.StatsVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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

        String aiSuggestion = "### 💡 AI 导师提示\n\n当前 AI 咨询人数过多（触发了 API 接口限流），暂时无法生成长篇学习建议文本。\n\n**不过别担心！** 系统已为您精准计算出了上方的**【真实掌握率雷达图】**以及**【核心错题盲区】**。请根据上方的雷达图短板和列出的错题进行针对性复习。稍后您可以点击“重新生成”按钮再次尝试！";
        try {
            // 尝试调用大模型
            String response = kimiAiService.callKimiAI(prompt);
            if (response != null && !response.isEmpty()) {
                aiSuggestion = response;
            }
        } catch (Exception e) {
            log.error("AI生成学情诊断时触发限制或网络异常 (不影响其他数据展示): ", e);
        }

        result.setAiSuggestion(aiSuggestion);

        return result;
    }

} 