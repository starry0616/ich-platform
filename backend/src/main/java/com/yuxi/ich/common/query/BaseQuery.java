package com.yuxi.ich.common.query;

/**
 * 分页查询基类。
 *
 * <p>所有列表查询 DTO 继承该类，即可获得统一的分页与排序参数：
 * <pre>
 * GET /api/project/list?pageNum=1&amp;pageSize=10&amp;sortField=announce_year&amp;sortOrder=desc
 * </pre>
 * 前端无需在每个接口单独处理分页参数。
 */
public class BaseQuery {

    /** 每页条数上限，防止一次拉取过多数据 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 页码，从 1 开始 */
    private Long pageNum = 1L;
    /** 每页条数，默认 10 */
    private Long pageSize = 10L;
    /** 排序字段（数据库列名，snake_case） */
    private String sortField;
    /** 排序方向：asc / desc */
    private String sortOrder = "desc";

    public Long getPageNum() {
        return (pageNum == null || pageNum < 1) ? 1L : pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
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

    /** 是否升序 */
    public boolean isAsc() {
        return "asc".equalsIgnoreCase(sortOrder);
    }
}
