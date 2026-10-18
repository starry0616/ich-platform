package com.yuxi.ich.article.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文化资讯。
 *
 * <p>表：cms_article。资讯来源分管理员发布与传承人投稿两类，
 * 均需审核后发布。
 */
@TableName("cms_article")
public class CmsArticle extends BaseEntity implements Serializable {

    /** 资讯标题 */
    private String title;
    /** 所属分类 id，关联 cms_article_category */
    private Long categoryId;
    /** 封面图地址 */
    private String coverUrl;
    /** 摘要（列表页展示） */
    private String summary;
    /** 正文（富文本） */
    private String content;
    /** 关联的非遗项目 id */
    private Long projectId;
    /** 作者用户 id */
    private Long authorId;
    /** 稿件来源说明 */
    private String source;
    /** 是否置顶：0 否，1 是 */
    private Integer isTop;
    /** 审核状态：0 待审核，1 已发布，2 已下架，3 已驳回 */
    private Integer auditStatus;
    /** 审核意见 */
    private String auditRemark;
    /** 浏览次数 */
    private Integer viewCount;
    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 分类名称：非数据库字段 */
    @TableField(exist = false)
    private String categoryName;
    /** 作者昵称：非数据库字段 */
    @TableField(exist = false)
    private String authorName;
    /** 关联项目名称：非数据库字段 */
    @TableField(exist = false)
    private String projectName;

    // ---------------- getter / setter ----------------

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getIsTop() {
        return isTop;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
