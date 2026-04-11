package com.back.exam.controller;


import com.back.exam.common.Result;
import com.back.exam.entity.Banner;
import com.back.exam.service.BannerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banners")
@CrossOrigin
@Tag(name = "轮播图管理", description = "轮播图相关操作，包括图片上传、轮播图增删改查、状态管理等功能")  // Swagger API分组
public class BannerController {
    @Autowired
    private BannerService bannerService;

    @PostMapping("/upload-image") 
    @Operation(summary = "上传轮播图图片", description = "将图片文件上传到MinIO服务器，返回可访问的图片URL")  // API描述
    public Result<String> uploadBannerImage(
            @Parameter(description = "要上传的图片文件，支持jpg、png、gif等格式，大小限制5MB") 
            @RequestParam("file") MultipartFile file) {
        //上传轮播图
        String fileUrl = bannerService.upLoadBannerImage(file);
        return Result.success(fileUrl,"操作成功");
    }
    
 
    @GetMapping("/active")
    @Operation(summary = "获取启用的轮播图", description = "获取状态为启用的轮播图列表，供前台首页展示使用")  // API描述
    public Result<List<Banner>> getActiveBanners() {
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Banner::getIsActive,true).orderByAsc(Banner::getSortOrder);
        List<Banner> bannerList = bannerService.list(queryWrapper);
        //log.info("查询首页展示轮播图成功，结果为{}", bannerList);//日志输出结果(lombok提供)
        return Result.success(bannerList);
    }
  
    @GetMapping("/list")
    @Operation(summary = "获取所有轮播图", description = "获取所有轮播图列表，包括启用和禁用的，供管理后台使用")  // API描述
    public Result<List<Banner>> getAllBanners() {
        //1.调用业务层查询轮播图列表
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Banner::getSortOrder);
        List<Banner> bannerList = bannerService.list(queryWrapper);
        //log.info("查询所有轮播图成功，结果为{}", bannerList);//日志输出结果(lombok提供)
        //2.将数据放入Result中返回
        return Result.success(bannerList);
    }
    

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取轮播图", description = "根据轮播图ID获取单个轮播图的详细信息")  // API描述  
    public Result<Banner> getBannerById(@Parameter(description = "轮播图ID") @PathVariable @NotEmpty Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            return Result.error("轮播图不存在");
        }
        return Result.success(banner);
    }
  
    @PostMapping("/add") 
    @Operation(summary = "添加轮播图", description = "创建新的轮播图，需要提供图片URL、标题、跳转链接等信息")  // API描述
    public Result<String> addBanner(@RequestBody @Validated Banner banner) {
        bannerService.save(banner);
        return Result.success("操作成功");
    }

    @PutMapping("/update")  
    @Operation(summary = "更新轮播图", description = "更新轮播图的信息，包括图片、标题、跳转链接、排序等")  // API描述
    public Result<String> updateBanner(@RequestBody @Validated Banner banner) {
        bannerService.updateById(banner);
        return Result.success("操作成功");
    }

    @DeleteMapping("/delete/{id}") 
    @Operation(summary = "删除轮播图", description = "根据ID删除指定的轮播图")  // API描述
    public Result<String> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        bannerService.removeById(id);
        return Result.success("操作成功");
    }
    

    @PutMapping("/toggle/{id}")  
    @Operation(summary = "切换轮播图状态", description = "启用或禁用指定的轮播图，禁用后不会在前台显示")  // API描述
    public Result<String> toggleBannerStatus(
            @Parameter(description = "轮播图ID") @PathVariable Long id, 
            @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive) {
        LambdaUpdateWrapper<Banner> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Banner::getId,id).set(Banner::getIsActive,isActive); //更新时间无需维护，数据库建表的时候设置好了
        bannerService.update(updateWrapper);
        return Result.success("操作成功");
    }
}