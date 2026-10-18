package com.yuxi.ich.article.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;

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
}
