package com.yuxi.ich.activity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 非遗活动。
 *
 * <p>表：ich_activity。本表是系统中<b>唯一存在并发写入竞争</b>的表：
 * 多个用户同时报名同一个活动时，都要扣减同一个名额计数。
 */
@TableName("ich_activity")
public class IchActivity extends BaseEntity implements Serializable {

    /** 活动名称 */
    private String title;
    /** 类型：1 展演，2 体验课堂，3 讲座，4 展览 */
    private Integer activityType;
    /** 封面图 */
    private String coverUrl;
    /** 关联非遗项目 */
    private Long projectId;
    /** 主办传承人 */
    private Long inheritorId;
    /** 活动详情 */
    private String content;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 活动地点 */
    private String address;
    /** 经度 */
    private java.math.BigDecimal longitude;
    /** 纬度 */
    private java.math.BigDecimal latitude;
    /** 名额上限 */
    private Integer quota;
    /** 已报名人数 */
    private Integer signed;
    /** 报名截止时间 */
    private LocalDateTime signupDeadline;

    /**
     * 乐观锁版本号。
     *
     * <p>配合已注册的 OptimisticLockerInnerInterceptor，
     * MyBatis-Plus 会在 UPDATE 时自动带上 version 条件并把版本 +1。
     * 并发扣减名额时，若版本已被他人改动，本次更新影响行数为 0，
     * 业务层据此判定「扣减失败」并重试，从而避免超卖。
     */
    @Version
    private Integer version;

    /** 状态：0 待审核，1 报名中，2 报名结束，3 进行中，4 已结束，5 已取消 */
    private Integer status;
    /** 审核意见 */
    private String auditRemark;

    /** 关联项目名称：非数据库字段 */
    @TableField(exist = false)
    private String projectName;
    /** 主办传承人姓名：非数据库字段 */
    @TableField(exist = false)
    private String inheritorName;
    /** 剩余名额：非数据库字段，由 quota - signed 计算 */
    @TableField(exist = false)
    private Integer remaining;

    // ---------------- getter / setter ----------------

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

    public Integer getSigned() {
        return signed;
    }

    public void setSigned(Integer signed) {
        this.signed = signed;
    }

    public LocalDateTime getSignupDeadline() {
        return signupDeadline;
    }

    public void setSignupDeadline(LocalDateTime signupDeadline) {
        this.signupDeadline = signupDeadline;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getInheritorName() {
        return inheritorName;
    }

    public void setInheritorName(String inheritorName) {
        this.inheritorName = inheritorName;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }
}
