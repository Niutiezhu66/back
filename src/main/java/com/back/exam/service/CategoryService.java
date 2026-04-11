package com.back.exam.service;

import com.back.exam.common.Result;
import com.back.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    //查询分类列表，同时查询对应分类的题目数量
    List<Category> findCategoryList();

    //查询分类树状列表
    List<Category> findCategoryTreeList();

    Result<Void> saveCategory(Category category);

    Result<Void> updateCategory(Category category);

    Result<Void> deleteCategory(Long id);
}