package com.yuxi.ich.article.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯分类。
 *
 * <p>表：cms_article_category。为固定少量字典数据，如
 * 非遗资讯、政策文件、传承人故事、活动报道、学术研究。
 */
@TableName("cms_article_category")
public class CmsArticleCategory extends BaseEntity implements Serializable {

    /** 分类名称 */
    private String name;
    /** 排序号 */
    private Integer sortNo;

    /** 分类表不维护更新时间，排除父类继承的 update_time 列映射（MySQL cms_article_category 无此列） */
    @TableField(exist = false)
    private LocalDateTime updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
