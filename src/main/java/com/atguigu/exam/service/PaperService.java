package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.vo.AiPaperVo;
import com.atguigu.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {
    //手动组卷
    Paper createPaper(PaperVo paperVo);

    //按规则自动组卷
    Paper aiCreatePaper(AiPaperVo aiPaperVo);

    //更新试卷信息
    Paper updatePaper(Integer id, PaperVo paperVo);

    //删除试卷
    void deletePaper(Integer id);

    //查询试卷详情
    Paper getPaperById(Integer id);
}