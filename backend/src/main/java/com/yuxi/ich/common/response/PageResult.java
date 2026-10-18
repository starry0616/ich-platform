package com.yuxi.ich.common.response;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页返回结构。
 *
 * <p>列表类接口的 data 统一为该结构，前端只需读取 total 与 records。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;
    /** 当前页数据 */
    private List<T> records;
    /** 当前页码，从 1 开始 */
    private Long pageNum;
    /** 每页条数 */
    private Long pageSize;
    /** 总页数 */
    private Long pages;

    public PageResult() {
        this.total = 0L;
        this.records = Collections.emptyList();
        this.pageNum = 1L;
        this.pageSize = 10L;
        this.pages = 0L;
    }

    public PageResult(Long total, List<T> records, Long pageNum, Long pageSize) {
        this.total = total == null ? 0L : total;
        this.records = records == null ? Collections.emptyList() : records;
        this.pageNum = pageNum == null ? 1L : pageNum;
        this.pageSize = (pageSize == null || pageSize <= 0) ? 10L : pageSize;
        this.pages = (this.total + this.pageSize - 1) / this.pageSize;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }
}
