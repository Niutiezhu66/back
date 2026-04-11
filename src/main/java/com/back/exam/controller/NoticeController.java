package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.Notice;
import com.back.exam.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/notices")
@CrossOrigin
@Tag(name = "公告管理", description = "系统公告相关操作，包括公告发布、编辑、删除、状态管理等功能")  // Swagger API分组
public class NoticeController {
    @Autowired
    private NoticeService noticeService;
    
    @GetMapping("/active")  
    @Operation(summary = "获取启用的公告", description = "获取状态为启用的公告列表，供前台首页展示使用")  // API描述
    public Result<List<Notice>> getActiveNotices() {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getIsActive,true).orderByDesc(Notice::getPriority);
        List<Notice> list = noticeService.list(queryWrapper);
        return Result.success(list);
    }
    

    @GetMapping("/latest")  
    @Operation(summary = "获取最新公告", description = "获取最新发布的公告列表，用于首页推荐展示")  // API描述
    public Result<List<Notice>> getLatestNotices(
            @Parameter(description = "限制数量", example = "5") @RequestParam(defaultValue = "5") int limit) {
        return noticeService.getLatestNotices(limit);
    }

    @GetMapping("/list")  
    @Operation(summary = "获取所有公告", description = "获取所有公告列表，包括启用和禁用的，供管理后台使用")  // API描述
    public Result<List<Notice>> getAllNotices() {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notice::getPriority);
        List<Notice> noticeList = noticeService.list(queryWrapper);
        return Result.success(noticeList);
    }
    

    @GetMapping("/{id}")  
    @Operation(summary = "根据ID获取公告", description = "根据公告ID获取单个公告的详细信息")  // API描述
    public Result<Notice> getNoticeById(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice != null) {
            return Result.success(notice);
        } else {
            return Result.error("公告不存在");
        }
    }
    
    @PostMapping("/add")  
    @Operation(summary = "发布新公告", description = "创建并发布新的系统公告")  // API描述
    public Result<String> addNotice(@RequestBody Notice notice) {
        boolean success = noticeService.save(notice);
        if (success){
            return Result.success(null);
        }else {
            return Result.error("添加失败");
        }
    }
    

    @PutMapping("/update")  
    @Operation(summary = "更新公告信息", description = "修改公告的内容、标题、类型等信息")  // API描述
    public Result<String> updateNotice(@RequestBody Notice notice) {
        Notice n = noticeService.getById(notice.getId());
        if (n == null) {
            return Result.error("该公告不存在");
        }
        noticeService.updateById(notice);
        return Result.success(null);
    }

    @DeleteMapping("/delete/{id}")  
    @Operation(summary = "删除公告", description = "根据ID删除指定的公告")  // API描述
    public Result<String> deleteNotice(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success(null);
    }
    

    @PutMapping("/toggle/{id}")  
    @Operation(summary = "切换公告状态", description = "启用或禁用指定的公告，禁用后不会在前台显示")  // API描述
    public Result<String> toggleNoticeStatus(
            @Parameter(description = "公告ID") @PathVariable Long id, 
            @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive) {

        LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notice::getId, id).set(Notice::getIsActive, isActive);
        noticeService.update(updateWrapper);
        return Result.success(null);
    }
} 