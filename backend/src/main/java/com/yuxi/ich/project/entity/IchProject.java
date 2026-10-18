package com.yuxi.ich.project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;

/**
 * 非遗项目。
 *
 * <p>表：ich_project。数据来源为国家级非遗代表性项目名录，
 * 其中 summary / content / history / status_desc / cover_url 为待补充的富文本与图片字段。
 */
@TableName("ich_project")
public class IchProject extends BaseEntity implements Serializable {

    /** 项目名称 */
    private String projectName;
    /** 项目编号，如 Ⅷ-12（罗马数字为类别号） */
    private String projectCode;
    /** 所属类别 id，关联 ich_category */
    private Long categoryId;
    /** 级别：1 国家级，2 省级，3 市级，4 县级 */
    private Integer level;
    /** 申报地区 / 流布地域 */
    private String region;
    /** 批次序号 1-5 */
    private Integer batchNo;
    /** 公布年份 */
    private Integer announceYear;
    /** 类型：新增项目 / 扩展项目 */
    private String type;
    /** 保护单位 */
    private String protectionUnit;
    /** 封面图地址 */
    private String coverUrl;
    /** 项目简介（列表页展示） */
    private String summary;
    /** 详细内容（富文本） */
    private String content;
    /** 历史渊源 */
    private String history;
    /** 传承现状 */
    private String statusDesc;
    /** 第一代表性传承人 id */
    private Long inheritorId;
    /** 审核状态：0 待审核，1 已发布，2 已下架，3 已驳回 */
    private Integer auditStatus;
    /** 审核意见 */
    private String auditRemark;
    /** 浏览次数 */
    private Integer viewCount;
    /** 收藏次数 */
    private Integer favoriteCount;
    /** 排序号 */
    private Integer sortNo;

    /**
     * 类别名称：非数据库字段，由联表查询填充，
     * 避免前端为了显示类别名再发一次请求。
     */
    @TableField(exist = false)
    private String categoryName;

    // ---------------- getter / setter ----------------

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(Integer batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getAnnounceYear() {
        return announceYear;
    }

    public void setAnnounceYear(Integer announceYear) {
        this.announceYear = announceYear;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProtectionUnit() {
        return protectionUnit;
    }

    public void setProtectionUnit(String protectionUnit) {
        this.protectionUnit = protectionUnit;
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

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public Long getInheritorId() {
        return inheritorId;
    }

    public void setInheritorId(Long inheritorId) {
        this.inheritorId = inheritorId;
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

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
