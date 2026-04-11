package com.back.exam.mapper;

import com.back.exam.entity.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    List<Notice> selectLatestNotices(int limit);
    
} 