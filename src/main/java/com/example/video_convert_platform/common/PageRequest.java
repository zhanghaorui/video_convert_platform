package com.example.video_convert_platform.common;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页查询请求基类
 * 提供标准化的分页参数
 * 
 */
@Data
public class PageRequest {

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;

    private String sortBy;
    
    private String sortOrder = "DESC";

    /**
     * 计算偏移量
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 检查是否有排序条件
     */
    public boolean hasSort() {
        return sortBy != null && !sortBy.trim().isEmpty();
    }
}
