package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.atguigu.exam.common.CacheConstants;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.mapper.PaperQuestionMapper;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.KimiAiService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.utils.ExcelUtil;
import com.atguigu.exam.utils.RedisUtils;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.QuestionImportVo;
import com.atguigu.exam.vo.QuestionQueryVo;
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
import java.util.ArrayList;
import java.util.List;
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
        QuestionAnswer questionAnswer = questionAnswerMapper.selectById(id);

        //4.将选项和答案赋给Question对象
        question.setChoices(questionChoices);
        question.setAnswer(questionAnswer);

        //5.redis缓存处理(在子线程处理即可)
        Thread thread = new Thread(() -> {
            incrementQuestionScore(id);
        });
        thread.start();

        //6.返回结果
        return Result.success(question);
    }

    private void incrementQuestionScore(Long questionId) {
        /**
         * 在 redis 中进行题目热度增加,实现题目热度榜
         * @param questionId
         */
        redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY,questionId,1);
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
        Thread thread = new Thread(()->{
            redisUtils.zRemove(CacheConstants.POPULAR_QUESTIONS_KEY,id);
        });
        thread.start();

        //5.返回
        return Result.success(null);
    }

    @Override
    public Result<List<Question>> getPopularQuestion(Integer size) {
        //用于存储热门题目
        List<Question> questionList = new ArrayList<>();

        //1.再redis中取出热门题目id(倒序)
        Set<Object> popularIds = redisUtils.zReverseRange(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size-1);

        //2.根据查询的id查询对应题目
        if(!ObjectUtils.isEmpty(popularIds)){
            //将id转化成Long类型
            List<Long> Ids = popularIds.stream()
                    .map(Object::toString)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            //查询对应题目（为保持顺序使用for循环）
            for (Long id : Ids) {
                Question question = getById(id);
                //校验：id存在，但题目已经被删除. redis和mysql数据不同步问题
                if (question!=null) questionList.add(question);
            }
        }

        //3.检查题目集合数量是否满足size
        int lack = size - questionList.size();
        if (lack > 0) { //热门题目数量不足，查询最新的 lack 个题目加入集合
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Question::getCreateTime);
            //不能包括已经查出来的题目
            List<Long> existIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
            if (!existIds.isEmpty()){
                queryWrapper.notIn(Question::getId, existIds);
            }
            //只查询 lack 条数据
            queryWrapper.last("limit "+lack);
            List<Question> list = list(queryWrapper);//新数据集合
            questionList.addAll(list); //加入原有集合
        }

        //4.给题目赋上答案和选项（选择题）
        questionList.forEach(question -> {
            Long id = question.getId(); //获取题目id
            //根据id查询题目答案
            LambdaQueryWrapper<QuestionAnswer> wrapper1 = new LambdaQueryWrapper<>();
            wrapper1.eq(QuestionAnswer::getQuestionId,id);
            QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(wrapper1);
            question.setAnswer(questionAnswer);

            //若是选择题，则获取选项
            if ("CHOICE".equals(question.getType())){
                LambdaQueryWrapper<QuestionChoice> wrapper2 = new LambdaQueryWrapper<>();
                wrapper2.eq(QuestionChoice::getQuestionId,id).orderByAsc(QuestionChoice::getSort);
                List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(wrapper2);
                question.setChoices(questionChoices);
            }
        });

        //5.返回
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