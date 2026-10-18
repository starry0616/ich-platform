package com.yuxi.ich.activity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 活动发布 / 修改请求体。
 */
public class ActivitySaveRequest {

    private Long id;

    @NotBlank(message = "请填写活动名称")
    @Size(max = 150, message = "活动名称不能超过 150 字")
    private String title;

    /** 1 展演，2 体验课堂，3 讲座，4 展览 */
    private Integer activityType;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    private Long projectId;

    /** 主办传承人档案 id（对应 ich_inheritor.id，不是用户 id） */
    private Long inheritorId;

    @Size(max = 5000, message = "活动详情过长")
    private String content;

    @NotNull(message = "请填写活动开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请填写活动结束时间")
    private LocalDateTime endTime;

    @Size(max = 200, message = "活动地点不能超过 200 字")
    private String address;

    private java.math.BigDecimal longitude;

    private java.math.BigDecimal latitude;

    @NotNull(message = "请填写名额上限")
    @Min(value = 1, message = "名额上限至少为 1")
    private Integer quota;

    /** 报名截止时间，不填则默认取活动开始时间 */
    private LocalDateTime signupDeadline;

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

    public Integer getActivityType() {
        return activityType;
    }

    public void setActivityType(Integer activityType) {
        this.activityType = activityType;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public java.math.BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(java.math.BigDecimal longitude) {
        this.longitude = longitude;
    }

    public java.math.BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(java.math.BigDecimal latitude) {
        this.latitude = latitude;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public LocalDateTime getSignupDeadline() {
        return signupDeadline;
    }

    public void setSignupDeadline(LocalDateTime signupDeadline) {
        this.signupDeadline = signupDeadline;
    }
}
