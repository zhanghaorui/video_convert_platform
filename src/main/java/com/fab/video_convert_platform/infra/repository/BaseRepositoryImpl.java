package com.fab.video_convert_platform.infra.repository;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Repository层异常处理基类
 * 统一处理数据访问异常
 * 
 * @author zhanghaorui
 */
@Slf4j
public abstract class BaseRepositoryImpl {

    /**
     * 执行数据库操作，统一处理异常
     */
    protected <T> T executeWithExceptionHandling(String operation, DatabaseOperation<T> dbOperation) {
        try {
            return dbOperation.execute();
        } catch (DuplicateKeyException e) {
            log.error("Duplicate key error in {}: {}", operation, e.getMessage());
            throw BusinessException.of(ErrorCode.DATABASE_ERROR, "数据已存在，请检查唯一性约束");
        } catch (DataAccessException e) {
            log.error("Database error in {}: {}", operation, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.DATABASE_ERROR, "数据库操作失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in {}: {}", operation, e.getMessage(), e);
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "系统内部错误");
        }
    }

    /**
     * 数据库操作函数式接口
     */
    @FunctionalInterface
    protected interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
}
