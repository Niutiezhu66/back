package com.back.exam.controller;

import com.back.exam.common.Result;
import com.back.exam.entity.Category;
import com.back.exam.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类管理", description = "题目分类相关操作，包括分类的增删改查、树形结构管理等功能")  // Swagger API分组
public class CategoryController {
    @Autowired
    private CategoryService categoryService;


    @GetMapping 
    @Operation(summary = "获取分类列表", description = "获取所有题目分类列表，包含每个分类下的题目数量统计")  // API描述
    public Result<List<Category>> getCategories() {
        List<Category> categoryList = categoryService.findCategoryList();
        return Result.success(categoryList);
    }


    @GetMapping("/tree") 
    @Operation(summary = "获取分类树形结构", description = "获取题目分类的树形层级结构，用于前端树形组件展示")  // API描述
    public Result<List<Category>> getCategoryTree() {
        List<Category> categoryList = categoryService.findCategoryTreeList();
        return Result.success(categoryList);
    }

    @PostMapping 
    @Operation(summary = "添加新分类", description = "创建新的题目分类，支持设置父分类实现层级结构")  // API描述
    public Result<Void> addCategory(@RequestBody Category category) {
        return categoryService.saveCategory(category);
    }


    @PutMapping  
    @Operation(summary = "更新分类信息", description = "修改分类的名称、描述、排序等信息")  // API描述
    public Result<Void> updateCategory(@RequestBody Category category) {
        return categoryService.updateCategory(category);
    }


    @DeleteMapping("/{id}")  
    @Operation(summary = "删除分类", description = "删除指定的题目分类，注意：删除前需确保分类下没有题目")  // API描述
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        return categoryService.deleteCategory(id);
    }
} 