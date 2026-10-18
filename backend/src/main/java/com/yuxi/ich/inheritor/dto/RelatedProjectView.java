package com.yuxi.ich.inheritor.dto;

import java.io.Serializable;

/**
 * 传承人详情中展示的「关联项目」轻量视图。
 *
 * <p>仅包含公开展示所需字段。数据来源为只读关联表
 * {@code ich_project_inheritor}，仅用于展示，不回写任何归属外键。
 */
public class RelatedProjectView implements Serializable {

    private Long id;
    private String projectName;
    private String projectCode;
    /** 级别：1 国家级，2 省级，3 市级，4 县级 */
    private Integer level;
    private String region;
    private Integer batchNo;
    private String type;
    private String coverUrl;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
