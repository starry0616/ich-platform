package com.yuxi.ich.inheritor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 传承人信息维护请求体（已认证传承人更新自己的主页）。
 *
 * <p>与申请请求的区别：不允许修改 name 与 certNo 等审核依据字段，
 * 只能维护简介、作品与联系方式等展示内容。
 */
public class InheritorUpdateRequest {

    @Size(max = 100, message = "所属地区不能超过 100 字")
    private String region;

    private Integer craftYears;

    @Size(max = 2000, message = "人物简介不能超过 2000 字")
    private String intro;

    @Size(max = 2000, message = "代表性作品不能超过 2000 字")
    private String works;

    @Size(max = 50, message = "联系方式不能超过 50 字")
    private String contact;

    @Size(max = 255, message = "证书图片地址过长")
    private String certImg;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getCraftYears() {
        return craftYears;
    }

    public void setCraftYears(Integer craftYears) {
        this.craftYears = craftYears;
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

    public String getCertImg() {
        return certImg;
    }

    public void setCertImg(String certImg) {
        this.certImg = certImg;
    }
}
