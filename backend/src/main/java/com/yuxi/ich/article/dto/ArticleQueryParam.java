package com.yuxi.ich.article.dto;

import com.yuxi.ich.common.query.BaseQuery;

/**
 * 资讯列表查询条件。
 *
 * <pre>
 * GET /api/article/list?categoryId=1&amp;keyword=非遗&amp;pageNum=1&amp;pageSize=10
 * </pre>
 */
public class ArticleQueryParam extends BaseQuery {

    /** 分类 id */
    private Long categoryId;
    /** 关联项目 id */
    private Long projectId;
    /** 作者用户 id */
    private Long authorId;
    /** 标题与摘要关键词，模糊匹配 */
    private String keyword;
    /**
     * 审核状态。前台固定传 1（只看已发布）；后台可传 null 看全部。
     */
    private Integer auditStatus;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }
}
