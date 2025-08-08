package com.fab.video_convert_platform.common;

/**
 * Unified error codes for API responses and exceptions.
 */
public enum ErrorCode {
    SUCCESS(0, "OK"),
    PARAM_ERROR(400, "Parameter error"),
    PROJECT_NOT_FOUND(1001, "Project config not found"),
    STORE_FILE_FAILED(1002, "Failed to store file"),
    FFMPEG_INTERRUPTED(1003, "FFmpeg interrupted"),
    SOURCE_FILE_NOT_FOUND(1004, "Source file not found"),
    MD5_REQUIRED(1005, "MD5 is required for MQ message"),
    MD5_MISMATCH(1006, "MD5 mismatch"),
    MQ_PROCESS_FAILED(1007, "Failed to process MQ file"),
    VIDEO_RESOLUTION_ERROR(1008, "Failed to parse video resolution"),
    SYSTEM_ERROR(500, "Internal server error");

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
