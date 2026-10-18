package com.yuxi.ich.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增 / 修改非遗项目的请求体。
 *
 * <p>校验注解在进入 Controller 方法前即生效，校验失败由全局异常处理器
 * 统一转换为 400 响应，Service 层无需再判空。
 */
public class ProjectSaveRequest {

    /** 修改时必填；新增时留空 */
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过 100 字")
    private String projectName;

    @Size(max = 50, message = "项目编号不能超过 50 字")
    private String projectCode;

    @NotNull(message = "请选择所属类别")
    private Long categoryId;

    private Integer level;

    @Size(max = 120, message = "申报地区不能超过 120 字")
    private String region;

    private Integer batchNo;

    private Integer announceYear;

    private String type;

    @Size(max = 150, message = "保护单位不能超过 150 字")
    private String protectionUnit;

    private String coverUrl;

    @Size(max = 500, message = "项目简介不能超过 500 字")
    private String summary;

    private String content;

    private String history;

    private String statusDesc;

    private Long inheritorId;

    private Integer sortNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
