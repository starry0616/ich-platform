package com.yuxi.ich.inheritor.dto;

import com.yuxi.ich.common.query.BaseQuery;

/**
 * 传承人列表查询条件。
 *
 * <pre>
 * GET /api/inheritor/list?level=1&amp;region=云南&amp;keyword=李&amp;pageNum=1&amp;pageSize=12
 * </pre>
 */
public class InheritorQueryParam extends BaseQuery {

    /** 认定级别：1 国家级 2 省级 3 市级 4 县级 */
    private Integer level;
    /** 所属地区，模糊匹配 */
    private String region;
    /** 关联项目 id */
    private Long projectId;
    /** 姓名关键词，模糊匹配 */
    private String keyword;
    /**
     * 认证状态。前台固定传 1（只看已认证）；后台可传 null 看全部。
     */
    private Integer certStatus;

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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(Integer certStatus) {
        this.certStatus = certStatus;
    }
}
