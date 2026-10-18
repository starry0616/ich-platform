package com.yuxi.ich.project.mapper;

/**
 * 按类别统计的投影结果。
 *
 * <p>用于数据看板的「各类别项目占比」图表。
 */
public class CategoryCount {

    private String categoryName;
    private Long num;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getNum() {
        return num;
    }

    public void setNum(Long num) {
        this.num = num;
    }
}
