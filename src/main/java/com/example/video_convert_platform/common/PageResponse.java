package com.example.video_convert_platform.common;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应基类
 * 提供标准化的分页返回结构
 * 
 */
@NoArgsConstructor
public class PageResponse<T> {

    private List<T> records;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;

    public PageResponse(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records == null ? null : new ArrayList<>(records);
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }

    // 防御性编程：返回副本避免内部表示暴露
    public List<T> getRecords() {
        return records == null ? null : new ArrayList<>(records);
    }

    // 防御性编程：存储副本避免外部修改
    public void setRecords(List<T> records) {
        this.records = records == null ? null : new ArrayList<>(records);
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    /**
     * 快速创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> records, Long total, PageRequest pageRequest) {
        return new PageResponse<>(records, total, pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    /**
     * 创建空分页响应
     */
    public static <T> PageResponse<T> empty(PageRequest pageRequest) {
        return new PageResponse<>(java.util.Collections.emptyList(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return pageNum < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return pageNum > 1;
    }
}
