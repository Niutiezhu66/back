package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Notice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 公告服务接口
 */
public interface NoticeService extends IService<Notice> {
    /**
     * 获取最新的几条公告
     * @param limit 限制数量
     * @return 公告列表
     */
    Result<List<Notice>> getLatestNotices(int limit);
} 