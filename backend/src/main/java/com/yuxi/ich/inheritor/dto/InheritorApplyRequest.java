package com.yuxi.ich.inheritor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 传承人资质申请请求体。
 *
 * <p>普通用户提交后进入待审核（cert_status = 0），管理员审核通过后
 * 该用户角色提升为 ROLE_INHERITOR。
 */
public class InheritorApplyRequest {

    @NotBlank(message = "请填写姓名")
    @Size(max = 50, message = "姓名不能超过 50 字")
    private String name;

    /** 性别：1 男，2 女 */
    @Min(value = 1, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer gender;

    /** 认定级别 */
    @Min(value = 1, message = "认定级别取值不合法")
    @Max(value = 4, message = "认定级别取值不合法")
    private Integer level;

    /** 所属地区 */
    @Size(max = 100, message = "所属地区不能超过 100 字")
    private String region;

    /** 代表性项目 id */
    private Long projectId;

    /** 从艺年限 */
    @Min(value = 0, message = "从艺年限不能为负数")
    private Integer craftYears;

    /** 认定证书编号 */
    @Size(max = 50, message = "认定编号不能超过 50 字")
    private String certNo;

    /** 证书图片地址 */
    @Size(max = 255, message = "证书图片地址过长")
    private String certImg;

    /** 人物简介 */
    @Size(max = 2000, message = "人物简介不能超过 2000 字")
    private String intro;

    /** 代表性作品 */
    @Size(max = 2000, message = "代表性作品不能超过 2000 字")
    private String works;

    /** 公开联系方式 */
    @Size(max = 50, message = "联系方式不能超过 50 字")
    private String contact;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getCraftYears() {
        return craftYears;
    }

    public void setCraftYears(Integer craftYears) {
        this.craftYears = craftYears;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getCertImg() {
        return certImg;
    }

    public void setCertImg(String certImg) {
        this.certImg = certImg;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getWorks() {
        return works;
    }

    public void setWorks(String works) {
        this.works = works;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
