package com.fab.videoproject.common;

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
}
