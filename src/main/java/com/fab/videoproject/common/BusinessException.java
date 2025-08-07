package com.fab.videoproject.common;

/**
 * Custom business exception with error code support.
 */
public class BusinessException extends RuntimeException {
    public static final int DEFAULT_SUCCESS_CODE = 0;
    public static final int DEFAULT_ERROR_CODE = -1;

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(DEFAULT_ERROR_CODE, message);
    }

    public BusinessException() {
        this(DEFAULT_ERROR_CODE, "Business exception");
    }

    public int getCode() {
        return code;
    }

    public static BusinessException error(String message) {
        return new BusinessException(DEFAULT_ERROR_CODE, message);
    }
}
