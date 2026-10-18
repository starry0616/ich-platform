package com.yuxi.ich.project.controller;

import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.project.entity.IchCategory;
import com.yuxi.ich.project.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 非遗类别接口（对应说明书接口 13）。
 *
 * <p>公开接口，供前端渲染筛选栏与首页类别导航。
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 类别列表（十大门类） */
    @GetMapping("/list")
    public Result<List<IchCategory>> list() {
        return Result.success(categoryService.listAll());
    }
}
