package com.yuxi.ich.project.dto;

import com.yuxi.ich.common.query.BaseQuery;

/**
 * 非遗项目列表查询条件。
 *
 * <p>对应接口 GET /api/project/list，所有字段均可选，支持组合筛选：
 * <pre>
 * /api/project/list?categoryId=8&amp;level=1&amp;region=云南&amp;keyword=陶瓷
 *                 &amp;batchNo=1&amp;pageNum=1&amp;pageSize=12&amp;sortField=view_count&amp;sortOrder=desc
 * </pre>
 */
public class ProjectQueryParam extends BaseQuery {

    /** 类别 id */
    private Long categoryId;
    /** 级别：1 国家级 2 省级 3 市级 4 县级 */
    private Integer level;
    /** 申报地区，模糊匹配 */
    private String region;
    /** 批次序号 1-5 */
    private Integer batchNo;
    /** 类型：新增项目 / 扩展项目 */
    private String type;
    /** 关键词，同时匹配项目名称与简介 */
    private String keyword;
    /**
     * 审核状态。
     * 前台查询固定传 1（只看已发布）；后台查询传 null 看全部。
     */
    private Integer auditStatus;
    /** 排序字段（白名单见 IchProjectMapper.xml）：view_count / favorite_count / announce_year / batch_no / project_name */
    private String sortField;
    /** 排序方向：asc / desc，默认 desc */
    private String sortOrder;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
