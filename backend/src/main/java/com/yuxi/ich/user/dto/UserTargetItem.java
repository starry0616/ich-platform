package com.yuxi.ich.user.dto;

import java.time.LocalDateTime;

/**
 * 我的收藏 / 浏览历史列表项（联表带出目标名称与封面）。
 */
public class UserTargetItem {

    /** 目标类型：1 项目，2 资讯，3 活动 */
    private Integer targetType;

    private Long targetId;

    /** 目标名称（项目名 / 资讯标题 / 活动名） */
    private String title;

    /** 封面或缩略图 */
    private String coverUrl;

    /** 摘要 */
    private String summary;

    /** 收藏时间 / 最近浏览时间 */
    private LocalDateTime time;

    /** 浏览历史专用：累计浏览次数 */
    private Integer viewCount;

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
}
