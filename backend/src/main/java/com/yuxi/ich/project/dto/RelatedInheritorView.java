package com.yuxi.ich.project.dto;

import java.io.Serializable;

/**
 * 项目详情中展示的「关联传承人」轻量视图。
 *
 * <p>仅包含公开展示所需字段，刻意不返回联系方式、证书图片、个人简介等敏感信息。
 * 数据来源为只读关联表 {@code ich_project_inheritor}，仅用于展示，
 * 不作为项目管理归属依据（归属仍以 {@code ich_project.inheritor_id} 为准）。
 */
public class RelatedInheritorView implements Serializable {

    private Long id;
    private String name;
    /** 级别：1 国家级，2 省级，3 市级，4 县级 */
    private Integer level;
    private String region;
    /** 认定编号（公开展示用，非证件图片） */
    private String certNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }
}
