package com.back.exam.service.impl;


import com.back.exam.entity.ExamRecord;
import com.back.exam.entity.Paper;
import com.back.exam.entity.PaperQuestion;
import com.back.exam.entity.Question;
import com.back.exam.mapper.ExamRecordMapper;
import com.back.exam.mapper.PaperMapper;
import com.back.exam.mapper.QuestionMapper;
import com.back.exam.service.PaperQuestionService;
import com.back.exam.service.PaperService;
import com.back.exam.vo.AiPaperVo;
import com.back.exam.vo.PaperVo;
import com.back.exam.vo.RuleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {
    @Autowired
    private PaperQuestionService paperQuestionService;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private ExamRecordMapper examRecordMapper;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Paper createPaper(PaperVo paperVo) {
        // 1. 校验是否传入了教师ID
        if (paperVo.getTeacherId() == null) {
            throw new RuntimeException("试卷创建失败：未能获取到教师身份信息！");
        }

        Paper paper = new Paper();
        // 这里的 copyProperties 会自动把 paperVo 里的 teacherId 复制到 paper 对象里
        BeanUtils.copyProperties(paperVo, paper);
        paper.setStatus("DRAFT");//设置默认状态
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();

        if (questions == null || questions.isEmpty()){ //试卷没有题目
            paper.setTotalScore(BigDecimal.valueOf(0)); //试卷总分
            paper.setQuestionCount(0); //题目数量
            save(paper); //保存题目
            return paper;
        } else { //试卷有题目
            // ... (下方保持你原有的逻辑不变)
            paper.setQuestionCount(questions.size());
            Optional<BigDecimal> totalScore = questions.values().stream().reduce(BigDecimal::add);
            paper.setTotalScore(totalScore.get());
            save(paper);

            Long paperId = paper.getId();
            List<PaperQuestion> paperQuestionList = new ArrayList<>();
            questions.forEach((key, value) -> {
                PaperQuestion paperQuestion = new PaperQuestion();
                paperQuestion.setPaperId(Math.toIntExact(paperId));
                paperQuestion.setQuestionId(Long.valueOf(key));
                paperQuestion.setScore(value);
                paperQuestionList.add(paperQuestion);
            });
            paperQuestionService.saveBatch(paperQuestionList);
            return paper;
        }
    }

    @Override
    public Paper aiCreatePaper(AiPaperVo aiPaperVo) {
        //1.完善试卷基本信息,保存试卷信息到数据库,主键会回显
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo, paper);
        paper.setStatus("DRAFT");//设置默认状态
        save(paper);//先保存，题目数量和总分组卷完成后更新

        int questionCount = 0; //记录试卷题目数
        BigDecimal totalScore = BigDecimal.ZERO; //记录试卷总分

        List<RuleVo> rules = aiPaperVo.getRules(); //获取组卷规则
        for (RuleVo rule : rules) {
            //遍历规则，若该条规则要抽取的题目数量为 0 则跳过
            if (rule.getCount() == 0) continue;
            //获取对应类型的题目
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Question::getType, rule.getType().name());
            queryWrapper.in(!ObjectUtils.isEmpty(rule.getCategoryIds()), Question::getCategoryId, rule.getCategoryIds());
            List<Question> questionAllList = questionMapper.selectList(queryWrapper);
            //若题目数量为 0 则说明没有该类型题目
            if (ObjectUtils.isEmpty(questionAllList)) continue;

            //题目条数和分数
            int realNumber = Math.min(rule.getCount(), questionAllList.size());
            questionCount += realNumber;
            totalScore = totalScore.add(BigDecimal.valueOf((long) realNumber *rule.getScore()));

            //随机选出题目
            Collections.shuffle(questionAllList);//随机打乱集合顺序
            List<Question> questionList = questionAllList.subList(0, realNumber);

            //将题目集合转成 PaperQuestion 并保存
            List<PaperQuestion> paperQuestionList = new ArrayList<>();
            for (Question question : questionList) {
                int paperId = Math.toIntExact(paper.getId());
                Long questionId = question.getId();
                BigDecimal score = BigDecimal.valueOf(rule.getScore());
                PaperQuestion paperQuestion = new PaperQuestion(paperId, questionId, score);
                paperQuestionList.add(paperQuestion);
            }
            //5.保存 paperQuestionList 到数据库
            paperQuestionService.saveBatch(paperQuestionList);
        }
        //补上总分和题目数
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(questionCount);
        //更新最初插入试卷表的数据 返回试卷
        updateById(paper);
        return paper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Paper updatePaper(Integer id, PaperVo paperVo) {
        //1.发布状态下的试卷无法更新
        Paper paper = getById(id);
        if (paper == null) {
            throw new RuntimeException("试卷不存在或已被删除");
        }
        if("PUBLISHED".equals(paper.getStatus())) {
            throw new RuntimeException("发布状态下的试卷无法更新");
        }

        //2.更新后试卷名称不能和其他的相同，但和自己原来的一样没关系
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Paper::getId, id).eq(Paper::getName, paperVo.getName());
        Long count = count(wrapper);
        if (count>0) {
            throw new RuntimeException("试卷不可与其他时间重名");
        }

        //3.更新试卷信息
        Long originalTeacherId = paper.getTeacherId();
        BeanUtils.copyProperties(paperVo, paper);
        paper.setTeacherId(originalTeacherId);
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();
        if (questions == null || questions.isEmpty()) {
            paper.setQuestionCount(0);
            paper.setTotalScore(BigDecimal.ZERO);
            updateById(paper);

            LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaperQuestion::getPaperId, id);
            paperQuestionService.remove(queryWrapper);
            return paper;
        }

        paper.setQuestionCount(questions.size());//新的题目数

        Optional<BigDecimal> totalScore = questions.values().stream().reduce(BigDecimal::add);//分数累加
        paper.setTotalScore(totalScore.orElse(BigDecimal.ZERO)); //试卷总分
        updateById(paper); //保存题目

        //4.将题目 map 映射为 PaperQuestion 对象
        List<PaperQuestion> paperQuestionList = new ArrayList<>();
        questions.forEach((key, value) -> {
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(id);
            paperQuestion.setQuestionId(Long.valueOf(key));
            paperQuestion.setScore(value);
            paperQuestionList.add(paperQuestion);
        });

        //4.保存paperQuestionList到数据库（先删除后重新插入）
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getPaperId, id);
        paperQuestionService.remove(queryWrapper);//先删除
        paperQuestionService.saveBatch(paperQuestionList);//后插入

        return paper;
    }

    @Override
    public void deletePaper(Integer id) {
        Paper paper = getById(id);
        if (paper == null) {
            throw new RuntimeException("试卷不存在或已被删除");
        }
        //1.发布状态下的试卷无法删除
        if("PUBLISHED".equals(paper.getStatus())) {
            throw new RuntimeException("发布状态下的试卷无法更新");
        }

        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getExamId,id);
        Long count = examRecordMapper.selectCount(queryWrapper);
        if (count>0) {
            throw new RuntimeException("当前试卷关联了考试记录无法删除");
        }

        //删除试卷表
        removeById(id);
        //删除对应题目关联表
        LambdaQueryWrapper<PaperQuestion> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(PaperQuestion::getPaperId, id);
        paperQuestionService.remove(queryWrapper2);
    }

    @Override
    public Paper getPaperById(Integer id) {
        //1. 单表java代码进行paper查询
        Paper paper = getById(id);
        //2. 校验paper == null -> 抛异常
        if (paper == null){
            throw new RuntimeException("指定id:%s试卷已经被删除，无法查看详情！".formatted(id));
        }
        //3. 根据paperid查询题目集合（中间，题目，答案，选项）
        List<Question> questionList = questionMapper.selectQuestionListByPaperId(id);
        //4. 校验题目集合 == null -> 赋空集合！ log->做好记录
        if (ObjectUtils.isEmpty(questionList)){
            paper.setQuestions(new ArrayList<Question>());
            log.warn("试卷中没有题目！可以进行试卷编辑！但是不能用于考试！！,对应试卷id：{}",id);
            return paper;
        }
        log.debug("题目信息排序前：{}",questionList);
        //对题目进行排序（选择 -> 判断 -> 简答）
        questionList.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()),typeToInt(o2.getType())));
        //进行paper题目集合赋值
        paper.setQuestions(questionList);
        return paper;
    }

    /**
     * 获取题目类型的排序顺序
     * @param type 题目类型
     * @return 排序序号
     */
    private int typeToInt(String type) {
        return switch (type) {
            case "CHOICE" -> 1; // 选择题
            case "JUDGE" -> 2;  // 判断题
            case "TEXT" -> 3;   // 简答题
            default -> 4;       // 其他类型
        };
    }

}