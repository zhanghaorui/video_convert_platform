package com.example.video_convert_platform.common;

/**
 * Application-wide constants for video operations and archive paths.
 */
public final class VideoConstants {

    private VideoConstants() {
    }

    public static final String SOURCE_CONTROLLER = "controller";
    /** Source identifier for MQ-driven tasks. */
    public static final String SOURCE_MQ = "mq";
    public static final int DEFAULT_VERSION_NO = 1;
    public static final String DIR_ORIGINAL = "original";
    public static final String DIR_CHUNK = "chunk";
    public static final String DIR_SLICE_PREFIX = "slice_";
    public static final String QUALITY_LOW = "low";
    public static final String QUALITY_NORMAL = "normal";
    public static final String FILE_TYPE_M3U8 = "M3U8";
    public static final String M3U8_NAME = "index.m3u8";
    public static final String FILE_TYPE_ORIGINAL = "ORIGINAL";
    public static final String QUALITY_ORIGINAL = DIR_ORIGINAL;

    /** 允许上传的视频文件扩展名白名单 */
    public static final String[] ALLOWED_VIDEO_EXTENSIONS = {
        "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp", "mpeg", "mpg"
    };

    /** 允许上传的文件类型白名单（MIME类型前缀） */
    public static final String[] ALLOWED_CONTENT_TYPES = {
        "video/"
    };

    /** 最大文件大小（500MB） */
    public static final long MAX_FILE_SIZE = 500 * 1024 * 1024L;
}
