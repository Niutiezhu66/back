package com.atguigu.exam.mapper;

import com.atguigu.exam.entity.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告Mapper接口
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    /**
     * 获取最新的几条公告
     * @param limit 限制数量
     * @return 公告列表
     */
    List<Notice> selectLatestNotices(int limit);
    
} 