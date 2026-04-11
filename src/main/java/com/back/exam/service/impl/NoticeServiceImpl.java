package com.back.exam.service.impl;

import com.back.exam.common.Result;
import com.back.exam.entity.Notice;
import com.back.exam.mapper.NoticeMapper;
import com.back.exam.service.NoticeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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