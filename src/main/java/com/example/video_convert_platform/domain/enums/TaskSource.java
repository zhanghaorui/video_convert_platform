package com.example.video_convert_platform.domain.enums;

/**
 * 任务来源枚举
 * 用于区分任务是通过HTTP请求还是MQ消息创建的
 *
 */
public enum TaskSource {

    /**
     * HTTP请求来源 - 需要回调
     */
    HTTP("controller"),

    /**
     * MQ消息来源 - 不需要回调
     */
    MQ("mq");

    private final String value;

    TaskSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 判断是否为HTTP来源
     */
    public boolean isHttp() {
        return this == HTTP;
    }

    /**
     * 判断是否为MQ来源
     */
    public boolean isMq() {
        return this == MQ;
    }

    /**
     * 从字符串值获取枚举
     */
    public static TaskSource fromValue(String value) {
        for (TaskSource source : values()) {
            if (source.value.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown task source: " + value);
    }
}
