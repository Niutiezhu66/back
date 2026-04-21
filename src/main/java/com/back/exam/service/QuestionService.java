package com.back.exam.service;

import com.back.exam.common.Result;
import com.back.exam.entity.Question;
import com.back.exam.vo.AiGenerateRequestVo;
import com.back.exam.vo.QuestionImportVo;
import com.back.exam.vo.QuestionQueryVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface QuestionService extends IService<Question> {

    //分页查询题目信息
    void queryQuestionListByPage(Page<Question> questionPage, QuestionQueryVo questionQueryVo);

    //根据id查题目详情,包括选项和答案
    Result<Question> queryQuestionById(Long id);

    //增加题目热度
    Result<String> incrementQuestionView(Long id);

    //新增题目
    Result<Question> saveQuestion(Question question);

    //更新题目
    Result<Question> updateQuestion(Question question);

    //删除题目
    Result<String> deleteQuestion(Long id);

    //获取热门题目
    Result<List<Question>> getPopularQuestion(Integer size);

    //预览excel模板
    Result<List<QuestionImportVo>> previewExcel(MultipartFile file) throws IOException;

    //批量导入题目
    String importQuestions(List<QuestionImportVo> questions);

    //AI生成题目
    List<QuestionImportVo> aiGenerateQuestion(AiGenerateRequestVo request) throws InterruptedException;
}