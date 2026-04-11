package com.back.exam.service;

import com.back.exam.common.Result;
import com.back.exam.entity.Notice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface NoticeService extends IService<Notice> {

    Result<List<Notice>> getLatestNotices(int limit);
} 