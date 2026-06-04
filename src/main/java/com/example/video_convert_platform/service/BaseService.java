package com.example.video_convert_platform.service;

import com.example.video_convert_platform.common.PageRequest;
import com.example.video_convert_platform.common.PageResponse;

/**
 * 基础CRUD服务接口
 * 定义通用的CRUD操作规范
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface BaseService<T, ID> {

    /**
     * 创建实体
     */
    T create(T entity);

    /**
     * 根据ID查询实体
     */
    T getById(ID id);

    /**
     * 更新实体
     */
    T update(T entity);

    /**
     * 根据ID删除实体
     */
    boolean deleteById(ID id);

    /**
     * 分页查询
     */
    PageResponse<T> page(PageRequest pageRequest);

    /**
     * 检查实体是否存在
     */
    boolean existsById(ID id);
}
