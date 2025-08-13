package com.fab.video_convert_platform.interfaces.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放URL信息DTO
 */
@Data
public class PlayUrlInfo {

    /**
     * 质量级别
     */
    private String quality;

    /**
     * 播放URL地址
     */
    private String playUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
