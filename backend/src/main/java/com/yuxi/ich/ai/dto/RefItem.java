package com.yuxi.ich.ai.dto;

/**
 * 引用来源项（接口 46 /api/ai/chat/{id}/refs）。
 */
public class RefItem {

    /** 文档标题 */
    private String title;

    /** 来源类型：1 项目，2 传承人，3 资讯，4 政策/术语 */
    private Integer sourceType;

    /** 来源业务对象 id */
    private Long sourceId;

    /** 跳转地址（前端路由） */
    private String url;

    /** 命中片段 */
    private String snippet;

    public RefItem() {
    }

    public RefItem(String title, Integer sourceType, Long sourceId, String url, String snippet) {
        this.title = title;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.url = url;
        this.snippet = snippet;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
