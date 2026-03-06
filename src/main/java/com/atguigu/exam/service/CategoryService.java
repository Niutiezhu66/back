package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    //查询分类列表，同时查询对应分类的题目数量
    List<Category> findCategoryList();

    //查询分类树状列表
    List<Category> findCategoryTreeList();

    //保存分类信息
    Result<Void> saveCategory(Category category);

    //更新分类
    Result<Void> updateCategory(Category category);

    //删除分类
    Result<Void> deleteCategory(Long id);
}