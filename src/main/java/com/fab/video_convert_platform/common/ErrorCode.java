package com.fab.video_convert_platform.common;

/**
 * 错误码枚举类
 * @author 张浩锐
 */
public enum ErrorCode {
    SUCCESS(0, "OK"),

    // 参数错误类 400-499
    PARAM_ERROR(400, "Parameter error"),
    INVALID_FILE_TYPE(401, "Invalid file type"),
    FILE_SIZE_EXCEEDED(402, "File size exceeded limit"),
    CHUNK_SIZE_INVALID(403, "Chunk size invalid"),

    // 项目配置错误类 1000-1099
    PROJECT_NOT_FOUND(1001, "Project config not found"),
    PROJECT_INACTIVE(1002, "Project is inactive"),

    // 文件存储错误类 1100-1199
    STORE_FILE_FAILED(1100, "Failed to store file"),
    FILE_NOT_FOUND(1101, "File not found"),
    FILE_COPY_FAILED(1102, "Failed to copy file"),
    FILE_DELETE_FAILED(1103, "Failed to delete file"),
    CHUNK_MERGE_FAILED(1104, "Failed to merge chunks"),
    FILE_PERMISSION_DENIED(1105, "File permission denied"),
    DISK_SPACE_INSUFFICIENT(1106, "Insufficient disk space"),

    // MD5校验错误类 1200-1299
    MD5_REQUIRED(1200, "MD5 is required"),
    MD5_MISMATCH(1201, "MD5 verification failed"),
    MD5_CALCULATION_FAILED(1202, "Failed to calculate MD5"),

    // FFmpeg处理错误类 1300-1399
    FFMPEG_NOT_FOUND(1300, "FFmpeg executable not found"),
    FFMPEG_TIMEOUT(1301, "FFmpeg processing timeout"),
    FFMPEG_INTERRUPTED(1302, "FFmpeg processing interrupted"),
    VIDEO_FORMAT_UNSUPPORTED(1303, "Unsupported video format"),
    VIDEO_CORRUPTED(1304, "Video file corrupted"),
    VIDEO_RESOLUTION_ERROR(1305, "Failed to parse video resolution"),
    FFMPEG_COMMAND_FAILED(1306, "FFmpeg command execution failed"),

    // 转码切片错误类 1400-1499
    TRANSCODE_FAILED(1400, "Video transcoding failed"),
    SLICE_FAILED(1401, "Video slicing failed"),
    M3U8_GENERATION_FAILED(1402, "M3U8 generation failed"),

    // MQ处理错误类 1500-1599
    MQ_MESSAGE_INVALID(1500, "Invalid MQ message format"),
    MQ_PROCESS_FAILED(1501, "Failed to process MQ message"),
    SOURCE_FILE_NOT_FOUND(1502, "Source file not found in MQ message"),

    // 回调错误类 1600-1699
    CALLBACK_FAILED(1600, "Callback notification failed"),
    CALLBACK_TIMEOUT(1601, "Callback timeout"),
    CALLBACK_URL_INVALID(1602, "Invalid callback URL"),

    // 数据库错误类 1700-1799
    DATABASE_ERROR(1700, "Database operation failed"),
    TASK_NOT_FOUND(1701, "Task not found"),
    TASK_STATUS_INVALID(1702, "Invalid task status"),

    // 系统错误类 5000-5999
    SYSTEM_ERROR(5000, "Internal server error"),
    THREAD_INTERRUPTED(5001, "Thread interrupted"),
    RESOURCE_EXHAUSTED(5002, "System resource exhausted"),
    CONCURRENT_LIMIT_EXCEEDED(5003, "Concurrent processing limit exceeded");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
