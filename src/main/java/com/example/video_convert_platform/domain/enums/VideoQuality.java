package com.example.video_convert_platform.domain.enums;

/**
 * Supported quality levels for output videos.
 */
public enum VideoQuality {
    LOW("low", 640, 360),
    NORMAL("normal", -1, -1);  // 使用-1表示原始分辨率

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

    /**
     * 判断是否使用原始分辨率
     */
    public boolean isOriginalResolution() {
        return width == -1 || height == -1;
    }
}
