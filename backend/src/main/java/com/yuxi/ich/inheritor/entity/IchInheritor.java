package com.yuxi.ich.inheritor.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;
import com.yuxi.ich.inheritor.dto.RelatedProjectView;

import java.io.Serializable;
import java.util.List;

/**
 * 非遗传承人。
 *
 * <p>表：ich_inheritor。数据来源为国家級非遗代表性传承人名录；
 * cert_no 存原始认定编号，全局唯一，用于与项目关联。
 */
@TableName("ich_inheritor")
public class IchInheritor extends BaseEntity implements Serializable {

    /** 关联的平台账号 id（历史传承人无账号，可为空） */
    private Long userId;
    /** 姓名 */
    private String name;
    /** 性别：1 男，2 女 */
    private Integer gender;
    /** 出生年份 */
    private Integer birthYear;
    /** 认定级别：1 国家级，2 省级，3 市级，4 县级 */
    private Integer level;
    /** 所属地区 */
    private String region;
    /** 代表性项目 id */
    private Long projectId;
    /** 从艺年限 */
    private Integer craftYears;
    /** 认定编号 */
    private String certNo;
    /** 证书图片地址 */
    private String certImg;
    /** 人物简介 */
    private String intro;
    /** 代表性作品 */
    private String works;
    /** 公开联系方式 */
    private String contact;
    /** 认证状态：0 待审核，1 已认证，2 已驳回 */
    private Integer certStatus;
    /** 主页访问量 */
    private Integer viewCount;

    /** 关联项目名称：非数据库字段，联表查询填充 */
    @TableField(exist = false)
    private String projectName;

    /** 关联网站账号用户名：非数据库字段 */
    @TableField(exist = false)
    private String username;

    /**
     * 关联项目列表：非数据库字段，由关联表只读查询填充，仅用于传承人详情展示。
     * 与单值 {@link #projectId} 及管理归属无关，不参与权限判定，也不回写该外键。
     */
    @TableField(exist = false)
    private List<RelatedProjectView> relatedProjects;

    // ---------------- getter / setter ----------------

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
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

    public Integer getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(Integer certStatus) {
        this.certStatus = certStatus;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RelatedProjectView> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<RelatedProjectView> relatedProjects) {
        this.relatedProjects = relatedProjects;
    }
}
