package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Notice;
import com.atguigu.exam.mapper.NoticeMapper;
import com.atguigu.exam.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 公告服务实现类
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public Result<List<Notice>> getLatestNotices(int limit) {
        try {
            List<Notice> notices = noticeMapper.selectLatestNotices(limit);
            return Result.success(notices);
        } catch (Exception e) {
            return Result.error("获取最新公告失败：" + e.getMessage());
        }
    }
} 