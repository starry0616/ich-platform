package com.yuxi.ich.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

/**
 * 首页推荐位表（sys_banner）。
 *
 * <p>支撑首页推荐位配置（说明书表 6-6 接口 66—67）。
 */
@TableName("sys_banner")
public class SysBanner extends BaseEntity {

    private String title;

    /** 图片地址 */
    private String imageUrl;

    /** 1 项目，2 资讯，3 活动，4 外链 */
    private Integer targetType;

    private Long targetId;

    /** 外链地址（target_type = 4 时使用） */
    private String linkUrl;

    private Integer sortNo;

    /** 1 启用，0 停用 */
    private Integer status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
