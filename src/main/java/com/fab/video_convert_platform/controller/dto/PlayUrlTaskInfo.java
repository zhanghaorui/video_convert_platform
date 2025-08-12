package com.fab.video_convert_platform.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 播放URL任务信息DTO
 */
@Data
public class PlayUrlTaskInfo {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务唯一标识
     */
    private String uuid;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 播放URL列表
     */
    private List<PlayUrlInfo> playUrls;

    // 防御性 getter/setter 方法
    public List<PlayUrlInfo> getPlayUrls() {
        return playUrls != null ? new ArrayList<>(playUrls) : null;
    }

    public void setPlayUrls(List<PlayUrlInfo> playUrls) {
        this.playUrls = playUrls != null ? new ArrayList<>(playUrls) : null;
    }
}
