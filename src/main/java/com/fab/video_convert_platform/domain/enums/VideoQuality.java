package com.fab.video_convert_platform.domain.enums;

/**
 * Supported quality levels for output videos.
 */
public enum VideoQuality {
    LOW("low", 640, 360),
    NORMAL("normal", ORIGINAL_RESOLUTION, ORIGINAL_RESOLUTION);

    private static final int ORIGINAL_RESOLUTION = -1;
    private final String name;
    private final int width;
    private final int height;

    VideoQuality(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
