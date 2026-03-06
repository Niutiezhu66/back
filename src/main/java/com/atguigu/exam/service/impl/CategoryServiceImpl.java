package com.atguigu.exam.service.impl;


import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<Category> findCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        //获取所有分类
        List<Category> categoryList = list(wrapper);

        //获取每个分类对应题目数量，放在Map里
        List<Map<Long, Long>> mapList = questionMapper.selectCategoryQuestionCount();
        //将集合List 用stream 流转成 Map
        Map<Long, Long> collect = mapList.stream().collect(Collectors.toMap(p -> p.get("category_id"), p -> p.get("count")));

        //将对应题目数量存入
        categoryList.forEach(category -> {
            Long id = category.getId();
            category.setCount(collect.getOrDefault(id, 0L));
        });
        //返回完整数据
        return categoryList;
    }

    @Override
    public List<Category> findCategoryTreeList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        //获取所有分类
        List<Category> categoryList = list(wrapper);

        //获取每个分类对应题目数量，放在Map里
        List<Map<Long, Long>> mapList = questionMapper.selectCategoryQuestionCount();
        //将集合List 用stream 流转成 Map
        Map<Long, Long> collect = mapList.stream().collect(Collectors.toMap(p -> p.get("category_id"), p -> p.get("count")));

        //将对应题目数量存入
        categoryList.forEach(category -> {
            Long id = category.getId();
            category.setCount(collect.getOrDefault(id, 0L));
        });

        //分类信息进行分组,返回 Map< parent_id, List<对应分类> >
        Map<Long, List<Category>> treeMap = categoryList.stream().collect(Collectors.groupingBy(Category::getParentId));

        //筛选分类信息
        List<Category> parentCategoryList = categoryList.stream().filter(p -> p.getParentId() == 0).collect(Collectors.toList());

        //给一级分类循环,获取子分类,并计算count
        parentCategoryList.forEach(parentCategory -> {
            Long id = parentCategory.getId();
            List<Category> childrenCategoryList = treeMap.getOrDefault(id, new ArrayList<>());
            parentCategory.setChildren(childrenCategoryList);
            Long sonCount = childrenCategoryList.stream().collect(Collectors.summingLong(Category::getCount));
            parentCategory.setCount(parentCategory.getCount() + sonCount);
        });

        //返回完整数据
        return parentCategoryList;
    }

    @Override
    public Result<Void> saveCategory(Category category) {
        Long parentId = category.getParentId();//所属一级分类
        String name = category.getName();//二级分类名称
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId).eq(Category::getName, name);
        Category c = getOne(wrapper);
        if (c != null) {
            return Result.error("该二级分类已经存在");
        }
        save(category);
        return Result.success("操作成功");
    }


    @Override
    public Result<Void> updateCategory(Category category) {
        //同一个分类下,可以和自己本身名字相同,但不能改后不能和其他分类名字相同
        Long id = category.getId();
        Long parentId = category.getParentId();//所属一级分类
        String name = category.getName();//二级分类名称
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        /**
         * 在同一个“家庭”（parentId 相同）里找：
         * 有没有其他分类（ne(Category::getId, id) 排除自己）
         * 已经叫这个名字了（eq(Category::getName, name)）
         */
        wrapper.eq(Category::getParentId, parentId)
                .ne(Category::getId, id)
                .eq(Category::getName, name);
        Category c = getOne(wrapper);
        if (c != null) {
            return Result.error("该二级分类已经存在");
        }
        updateById(category);
        return Result.success("操作成功");
    }

    @Override
    public Result<Void> deleteCategory(Long id) {
        //1.判断是不是一级分类,一级分类不能删除
        Category category = getById(id);
        if (category == null) { return Result.error("该分类不存在");}
        if(category.getParentId() == 0){ return Result.error("一级分类不可删除");}
        //2.判断有无关联的题目
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getCategoryId, id);
        Long count = questionMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("删除失败,该分类下还有"+count+"个相关题目");
        }
        //3.上面做完之后才可以删除
        removeById(id);
        return Result.success("操作成功");
    }
}