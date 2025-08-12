package com.fab.video_convert_platform.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 播放URL查询响应DTO
 */
@Data
public class PlayUrlResponse {

    /**
     * 项目编号
     */
    private String projectNo;

    /**
     * 受试者编码
     */
    private String patientCode;

    /**
     * 访视点
     */
    private String tpStage;

    /**
     * 任务列表
     */
    private List<PlayUrlTaskInfo> tasks;

    /**
     * 任务总数
     */
    private Integer totalTasks;

    /**
     * 播放URL总数
     */
    private Integer totalPlayUrls;
}
