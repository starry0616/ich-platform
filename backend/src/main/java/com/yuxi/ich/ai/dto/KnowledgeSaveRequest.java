package com.yuxi.ich.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 知识库文档保存请求（接口 48—49）。
 */
public class KnowledgeSaveRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    private Long sourceId;

    /** 类别元数据（检索过滤） */
    private Long categoryId;

    /** 地域元数据（检索过滤） */
    private String region;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
