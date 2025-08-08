package com.fab.videoproject.service;

/**
 * Service for recording task logs and errors.
 */
public interface ITaskLogService {

    /**
     * Record normal info log for a task.
     *
     * @param taskId task identifier
     * @param message log message
     */
    void info(Long taskId, String message);

    /**
     * Record error log for a task.
     *
     * @param taskId task identifier
     * @param errorMsg error message
     */
    void error(Long taskId, String errorMsg);
}

