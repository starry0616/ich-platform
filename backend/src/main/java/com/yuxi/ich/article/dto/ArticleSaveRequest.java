package com.yuxi.ich.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 资讯发布 / 修改请求体。
 */
public class ArticleSaveRequest {

    /** 修改时必填 */
    private Long id;

    @NotBlank(message = "请填写资讯标题")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @NotNull(message = "请选择资讯分类")
    private Long categoryId;

    @Size(max = 255, message = "封面图地址过长")
    private String coverUrl;

    @Size(max = 500, message = "摘要不能超过 500 字")
    private String summary;

    @NotBlank(message = "请填写资讯正文")
    private String content;

    /** 关联的非遗项目 id，可选 */
    private Long projectId;

    @Size(max = 200, message = "来源说明不能超过 200 字")
    private String source;

    /** 是否置顶，仅管理员可设置 */
    private Integer isTop;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
}
