package com.back.exam.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.back.exam.common.CacheConstants;
import com.back.exam.common.Result;
import com.back.exam.entity.PaperQuestion;
import com.back.exam.entity.Question;
import com.back.exam.entity.QuestionAnswer;
import com.back.exam.entity.QuestionChoice;
import com.back.exam.mapper.AnswerRecordMapper;
import com.back.exam.mapper.CategoryMapper;
import com.back.exam.mapper.PaperQuestionMapper;
import com.back.exam.mapper.QuestionAnswerMapper;
import com.back.exam.mapper.QuestionChoiceMapper;
import com.back.exam.mapper.QuestionMapper;
import com.back.exam.service.KimiAiService;
import com.back.exam.service.QuestionService;
import com.back.exam.utils.ExcelUtil;
import com.back.exam.utils.RedisUtils;
import com.back.exam.vo.AiGenerateRequestVo;
import com.back.exam.vo.QuestionImportVo;
import com.back.exam.vo.QuestionQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private KimiAiService kimiAiService;

    //分页查询题目信息
    @Override
    public void queryQuestionListByPage(Page<Question> questionPage, QuestionQueryVo questionQueryVo) {
        questionMapper.selectQuestionPage(questionPage,questionQueryVo);
    }

    //根据id查题目详情,包括选项和答案
    @Override
    public Result<Question> queryQuestionById(Long id) {
        //1.查询题目详情
        Question question = getById(id);
        if(question==null){return Result.error("该题目不存在");}

        //2.查询题目对应选项(选择题才有)
        List<QuestionChoice> questionChoices = null;
        if("CHOICE".equals(question.getType())){
            LambdaQueryWrapper<QuestionChoice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(QuestionChoice::getQuestionId, id);
            questionChoices = questionChoiceMapper.selectList(queryWrapper);
        }

        //3.查询题目对应答案
        LambdaQueryWrapper<QuestionAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.eq(QuestionAnswer::getQuestionId, id);
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(answerWrapper);

        //4.将选项和答案赋给Question对象
        question.setChoices(questionChoices);
        question.setAnswer(questionAnswer);
        fillQuestionViewCount(question);

        //5.返回结果
        return Result.success(question);
    }

    @Override
    public Result<String> incrementQuestionView(Long id) {
        Question question = getById(id);
        if (question == null) {
            return Result.error("该题目不存在");
        }
        incrementQuestionScore(id);
        return Result.success("热度更新成功");
    }

    private void incrementQuestionScore(Long questionId) {
        /**
         * 在 redis 中进行题目热度增加,实现题目热度榜
         * @param questionId
         */
        String weeklyKey = getCurrentWeekPopularQuestionsKey();
        redisUtils.zIncrementScore(weeklyKey, questionId, 1);
        redisUtils.expire(weeklyKey, getCurrentWeekExpireSeconds());
    }

    private void fillQuestionViewCount(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        Double score = redisUtils.zScore(getCurrentWeekPopularQuestionsKey(), question.getId());
        question.setViewCount(score == null ? 0 : score.intValue());
    }

    private void fillQuestionViewCount(List<Question> questionList) {
        questionList.forEach(this::fillQuestionViewCount);
    }

    private void fillQuestionCategoryName(Question question) {
        if (question == null) {
            return;
        }
        if (question.getCategoryName() == null && question.getCategory() != null) {
            question.setCategoryName(question.getCategory().getName());
            return;
        }
        if (question.getCategoryName() == null && question.getCategoryId() != null) {
            com.back.exam.entity.Category category = categoryMapper.selectById(question.getCategoryId());
            question.setCategoryName(category == null ? null : category.getName());
        }
    }

    private void fillQuestionCategoryName(List<Question> questionList) {
        questionList.forEach(this::fillQuestionCategoryName);
    }

    private void fillQuestionCorrectRate(Question question) {
        if (question == null || question.getId() == null) {
            return;
        }
        LocalDateTime weekStart = getCurrentWeekStart();
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        int numerator = 0;
        int denominator = 0;
        List<Map<String, Object>> stats = answerRecordMapper.selectQuestionCorrectRateStats(question.getId(), weekStart, weekEnd);
        if (!stats.isEmpty()) {
            Map<String, Object> stat = stats.get(0);
            numerator = stat.get("numerator") == null ? 0 : new BigDecimal(stat.get("numerator").toString()).intValue();
            denominator = stat.get("denominator") == null ? 0 : new BigDecimal(stat.get("denominator").toString()).intValue();
        }

        int correctRate = denominator == 0 ? 0 : (int) Math.round((double) numerator * 100 / denominator);
        question.setCorrectRate(correctRate);
    }

    private void fillQuestionCorrectRate(List<Question> questionList) {
        questionList.forEach(this::fillQuestionCorrectRate);
    }

    private void enrichPopularQuestion(Question question) {
        Long id = question.getId();
        LambdaQueryWrapper<QuestionAnswer> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(QuestionAnswer::getQuestionId,id);
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(wrapper1);
        question.setAnswer(questionAnswer);

        if ("CHOICE".equals(question.getType())){
            LambdaQueryWrapper<QuestionChoice> wrapper2 = new LambdaQueryWrapper<>();
            wrapper2.eq(QuestionChoice::getQuestionId,id).orderByAsc(QuestionChoice::getSort);
            List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(wrapper2);
            question.setChoices(questionChoices);
        }

        fillQuestionCategoryName(question);
        fillQuestionViewCount(question);
        fillQuestionCorrectRate(question);
    }

    private void enrichPopularQuestionList(List<Question> questionList) {
        questionList.forEach(this::enrichPopularQuestion);
    }

    private String getCurrentWeekPopularQuestionsKey() {
        return CacheConstants.POPULAR_QUESTIONS_KEY + ":" + getCurrentWeekToken();
    }

    private String getCurrentWeekToken() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return weekStart.toString();
    }

    private LocalDateTime getCurrentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    }

    private long getCurrentWeekExpireSeconds() {
        LocalDateTime nextWeekStart = getCurrentWeekStart().plusWeeks(1);
        long seconds = java.time.Duration.between(LocalDateTime.now(), nextWeekStart).getSeconds();
        return Math.max(seconds, CacheConstants.WEEKLY_STATS_EXPIRE_SECONDS);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<Question> saveQuestion(Question question) {
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getTitle,question.getTitle());
        long count = count(queryWrapper);
        if(count>0){
            return Result.error("该题目已存在");
        }
        //是选择题保存 题目+选项+自行提取选项中的答案保存
        //非选择题保存 题目+答案
        save(question); //mp提供的方法，会自动主键回显
        Long questionId = question.getId();
        String questionType = question.getType();
        //答案对象,选择题answer==null;非选择answer有答案
        QuestionAnswer questionAnswer = question.getAnswer();
        questionAnswer.setQuestionId(questionId);

        if("CHOICE".equals(questionType)){ //如果是选择题
            List<QuestionChoice> questionChoices = question.getChoices();
            int t = 0; char [] turn = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T'};
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < questionChoices.size(); i++) {
                QuestionChoice questionChoice = questionChoices.get(i);
                questionChoice.setQuestionId(questionId);
                questionChoice.setSort(i); //0,1,2,3 对应 A,B,C,D
                //先向选项表插入选项
                questionChoiceMapper.insert(questionChoice);
                if (!questionChoice.getIsCorrect()) continue;
                if (t==0) answer.append(turn[i]);
                else answer.append(",").append(turn[i]);
                t++;
            }
            //存入答案
            questionAnswer.setAnswer(answer.toString());
        }
        questionAnswerMapper.insert(questionAnswer);
        return Result.success(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result<Question> updateQuestion(Question question) {
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getTitle,question.getTitle()) //题目相同
                    .ne(Question::getId,question.getId()); //且不是自己
        long count = count(queryWrapper);
        if(count>0){
            return Result.error("该题目已存在");
        }
        updateById(question);//更新题目

        Long questionId = question.getId();
        String questionType = question.getType();
        //新建答案对象,选择题answer==null;非选择answer有答案
        QuestionAnswer questionAnswer = question.getAnswer();

        if("CHOICE".equals(questionType)){  //如果是选择题
            //先删除再重新插入实现更新
            LambdaQueryWrapper<QuestionChoice> qWrapper = new LambdaQueryWrapper<>();
            qWrapper.eq(QuestionChoice::getQuestionId,questionId);
            questionChoiceMapper.delete(qWrapper);

            List<QuestionChoice> questionChoiceList = question.getChoices();
            int t = 0; char [] turn = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T'};
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < questionChoiceList.size(); i++) {
                QuestionChoice questionChoice = questionChoiceList.get(i);
                questionChoice.setQuestionId(questionId);
                questionChoice.setSort(i); //0,1,2,3... 对应 A,B,C,D...
                //先向选项表插入选项
                questionChoice.setId(null); //要先清空原来的主键等，否则会插入失败
                questionChoice.setCreateTime(null);
                questionChoice.setUpdateTime(null);
                questionChoiceMapper.insert(questionChoice);
                if (!questionChoice.getIsCorrect()) continue;
                if (t==0) answer.append(turn[i]);
                else answer.append(",").append(turn[i]);
                t++;
            }
            //存入答案
            questionAnswer.setAnswer(answer.toString());
        }
        questionAnswer.setCreateTime(null);
        questionAnswer.setUpdateTime(null);
        questionAnswerMapper.updateById(questionAnswer);
        return Result.success(null);
    }

    @Override
    public Result<String> deleteQuestion(Long id) {
        //1.检查是否有关联的试卷，有则删除失败
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getQuestionId,id);
        Long count = paperQuestionMapper.selectCount(queryWrapper);
        if (count>0) return Result.error("该题目已被应用于试卷中,无法删除");

        //2.删除题目本身
        removeById(id);

        //3.删除对应的 答案 和 选项（选择题）
        LambdaQueryWrapper<QuestionAnswer> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(QuestionAnswer::getQuestionId,id);
        questionAnswerMapper.delete(wrapper1);

        LambdaQueryWrapper<QuestionChoice> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(QuestionChoice::getQuestionId,id);
        questionChoiceMapper.delete(wrapper2);

        //4.删除 redis 缓存
        Thread thread = new Thread(() -> {
            redisUtils.zRemove(getCurrentWeekPopularQuestionsKey(), id);
        });
        thread.start();

        //5.返回
        return Result.success(null);
    }

    @Override
    public Result<List<Question>> getPopularQuestion(Integer size) {
        List<Question> questionList = new ArrayList<>();
        Set<Object> popularIds = redisUtils.zReverseRange(getCurrentWeekPopularQuestionsKey(), 0, size - 1);

        if (!ObjectUtils.isEmpty(popularIds)) {
            List<Long> ids = popularIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            for (Long id : ids) {
                Question question = questionMapper.selectQuestionWithCategory(id);
                if (question != null) {
                    questionList.add(question);
                }
            }
        }

        int lack = size - questionList.size();
        if (lack > 0) {
            LocalDateTime weekStart = getCurrentWeekStart();
            List<Question> fallbackList = answerRecordMapper.selectWeeklyPopularFallbackQuestions(
                    lack,
                    weekStart,
                    weekStart.plusWeeks(1)
            );
            Map<Long, Question> merged = new java.util.LinkedHashMap<>();
            questionList.forEach(question -> merged.put(question.getId(), question));
            fallbackList.forEach(question -> merged.putIfAbsent(question.getId(), question));
            questionList = new ArrayList<>(merged.values());
        }

        if (questionList.size() < size) {
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Question::getCreateTime);
            List<Long> existIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
            if (!existIds.isEmpty()) {
                queryWrapper.notIn(Question::getId, existIds);
            }
            queryWrapper.last("limit " + (size - questionList.size()));
            List<Question> latestList = list(queryWrapper);
            questionList.addAll(latestList);
        }

        enrichPopularQuestionList(questionList);
        questionList = questionList.stream()
                .limit(size)
                .collect(Collectors.toList());

        return Result.success(questionList);
    }

    @Override
    public Result<List<QuestionImportVo>> previewExcel(MultipartFile file) throws IOException {
        //1.文件校验（非空、xls、xlsx 结尾）
        if (file.isEmpty()) return Result.error("导入题目为空");

        String originalFilename = file.getOriginalFilename();
        if (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls")) return Result.error("文件格式错误");

        //2.文件解析为Vo
        List<QuestionImportVo> questionImportVoList = ExcelUtil.parseExcel(file);

        //3.返回
        return Result.success(questionImportVoList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String importQuestions(List<QuestionImportVo> questions) {
        //1.集合非空判断
        if (questions.isEmpty()) return "题目为空";

        //2.定义服务降级的代码结构
        int successNum = 0; // 导入成功的数量
        for (QuestionImportVo questionImportVo : questions) {
            //3.循环中，进行 Vo --> question 实体类的转换，再保存题目
            try {
                Question question = new Question();
                BeanUtils.copyProperties(questionImportVo, question);
                //是选择题，给选项赋值
                if ("CHOICE".equals(questionImportVo.getType())){
                    List<QuestionChoice> questionChoices = new ArrayList<>();
                    List<QuestionImportVo.ChoiceImportDto> choices = questionImportVo.getChoices();
                    for (QuestionImportVo.ChoiceImportDto choice : choices) {
                        QuestionChoice questionChoice = new QuestionChoice();
                        BeanUtils.copyProperties(choice, questionChoice);
                        questionChoices.add(questionChoice); // 添加该选项
                    }
                    //选择题选项赋值给 question 对象
                    question.setChoices(questionChoices);
                }

                //给题目答案赋值
                QuestionAnswer questionAnswer = new QuestionAnswer();
                if ("JUDGE".equals(questionImportVo.getType())){ //判断题的 true 要大写
                    questionAnswer.setAnswer(questionImportVo.getAnswer().toUpperCase());
                }else {
                    questionAnswer.setAnswer(questionImportVo.getAnswer());
                }
                questionAnswer.setKeywords(questionImportVo.getKeywords());

                question.setAnswer(questionAnswer);

                //保存题目
                saveQuestion(question);

                successNum++;

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        //4.返回结果
        return "题目导入结束，共 " + questions.size() + " 条成功导入 " + successNum + " 条";
    }

    @Override
    public List<QuestionImportVo> aiGenerateQuestion(AiGenerateRequestVo request) throws InterruptedException {
        //1.生成对应提示词
        String prompt = kimiAiService.buildPrompt(request);

        //2.调用封装好的方法获取结果
        String response = kimiAiService.callKimiAI(prompt);

        //3.进行结果解析
        int startIndex = response.indexOf("```json");
        int endIndex = response.lastIndexOf("```");

        if(startIndex != -1 && endIndex != -1 && startIndex < endIndex){
            //返回的结构正确,截取字符串
            String resultJson = response.substring(startIndex + 7, endIndex);

            JSONObject jsonObject = JSONObject.parseObject(resultJson);
            JSONArray questions = jsonObject.getJSONArray("questions");
            List<QuestionImportVo> questionImportVoList = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                //循环解析JSON对象
                JSONObject q = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(q.getString("title"));
                questionImportVo.setType(q.getString("type"));
                questionImportVo.setMulti(q.getBoolean("multi"));
                questionImportVo.setCategoryId(request.getCategoryId());
                questionImportVo.setDifficulty(q.getString("difficulty"));
                questionImportVo.setScore(q.getInteger("score"));
                questionImportVo.setAnalysis(q.getString("analysis"));
                questionImportVo.setAnswer(q.getString("answer"));

                //若是选择题还有赋值选项
                if ("CHOICE".equals(questionImportVo.getType())){
                    List<QuestionImportVo.ChoiceImportDto> choiceImportDtoList = new ArrayList<>();

                    JSONArray choices = q.getJSONArray("choices");
                    for (int j = 0; j < choices.size(); j++) {
                        QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                        choiceImportDto.setContent(choices.getJSONObject(j).getString("content"));
                        choiceImportDto.setIsCorrect(choices.getJSONObject(j).getBoolean("isCorrect"));
                        choiceImportDto.setSort(choices.getJSONObject(j).getInteger("sort"));
                        choiceImportDtoList.add(choiceImportDto);
                    }
                    questionImportVo.setChoices(choiceImportDtoList);
                }
                questionImportVoList.add(questionImportVo);
            }
            return questionImportVoList;
        }
        //返回不正确
        throw new RuntimeException("ai生成题目结构错误，无法解析");
    }




}