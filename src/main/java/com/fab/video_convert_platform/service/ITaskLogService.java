package com.fab.video_convert_platform.service;

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
     * Record normal info log for a task with format arguments.
     *
     * @param taskId task identifier
     * @param messageTemplate log message template with {} placeholders
     * @param args arguments to replace placeholders
     */
    void info(Long taskId, String messageTemplate, Object... args);

    /**
     * Record error log for a task.
     *
     * @param taskId task identifier
     * @param errorMsg error message
     */
    void error(Long taskId, String errorMsg);

    /**
     * Record error log for a task with format arguments.
     *
     * @param taskId task identifier
     * @param messageTemplate error message template with {} placeholders
     * @param args arguments to replace placeholders
     */
    void error(Long taskId, String messageTemplate, Object... args);
}
