package com.yuxi.ich.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;

/**
 * 非遗类别（十大门类）。
 *
 * <p>表：ich_category。category_code 存罗马数字编号（Ⅰ—Ⅹ），
 * 与项目编号（如 Ⅷ-12）的前缀对应。
 */
@TableName("ich_category")
public class IchCategory extends BaseEntity implements Serializable {

    /** 类别名称，如 传统技艺 */
    private String categoryName;
    /** 罗马数字编号，如 Ⅷ */
    private String categoryCode;
    /** 父级类别 id，0 表示顶层 */
    private Long parentId;
    /** 排序号 */
    private Integer sortNo;
    /** 图标地址 */
    private String icon;
    /** 状态：0 停用，1 启用 */
    private Integer status;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
