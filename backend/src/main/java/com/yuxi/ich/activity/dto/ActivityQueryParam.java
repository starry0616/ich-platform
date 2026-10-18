package com.yuxi.ich.activity.dto;

import com.yuxi.ich.common.query.BaseQuery;

/**
 * 活动列表查询条件。
 *
 * <pre>
 * GET /api/activity/list?status=1&amp;projectId=1&amp;keyword=展演&amp;pageNum=1&amp;pageSize=10
 * </pre>
 */
public class ActivityQueryParam extends BaseQuery {

    /** 状态：1 报名中 2 报名结束 3 进行中 4 已结束 */
    private Integer status;
    /** 活动类型：1 展演 2 体验课堂 3 讲座 4 展览 */
    private Integer activityType;
    /** 关联非遗项目 id */
    private Long projectId;
    /** 主办传承人 id */
    private Long inheritorId;
    /** 标题关键词 */
    private String keyword;
    /**
     * 是否只返回「可报名」的活动（报名中且未过截止时间）。
     * 前台首页用该条件筛出可报名的活动。
     */
    private Boolean onlyOpen;
    /**
     * 是否只返回已通过审核的活动。
     * 前台固定传 true；后台传 false 以查看待审核活动。
     */
    private Boolean onlyPublished;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getActivityType() {
        return activityType;
    }

    public void setActivityType(Integer activityType) {
        this.activityType = activityType;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getInheritorId() {
        return inheritorId;
    }

    public void setInheritorId(Long inheritorId) {
        this.inheritorId = inheritorId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getOnlyOpen() {
        return onlyOpen;
    }

    public void setOnlyOpen(Boolean onlyOpen) {
        this.onlyOpen = onlyOpen;
    }

    public Boolean getOnlyPublished() {
        return onlyPublished;
    }

    public void setOnlyPublished(Boolean onlyPublished) {
        this.onlyPublished = onlyPublished;
    }
}
