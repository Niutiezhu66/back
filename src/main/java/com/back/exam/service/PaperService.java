package com.back.exam.service;

import com.back.exam.entity.Paper;
import com.back.exam.vo.AiPaperVo;
import com.back.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.IService;


public interface PaperService extends IService<Paper> {

    Paper createPaper(PaperVo paperVo);

    Paper aiCreatePaper(AiPaperVo aiPaperVo);

    Paper updatePaper(Integer id, PaperVo paperVo);

    void deletePaper(Integer id);

    Paper getPaperById(Integer id);
}