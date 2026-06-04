package com.example.video_convert_platform.interfaces.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放URL查询响应DTO
 *
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
     * 访视描述（与tpStage互斥）
     */
    private String visit; // 新增

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

    // 防御性 getter/setter 方法
    public List<PlayUrlTaskInfo> getTasks() {
        return tasks != null ? new ArrayList<>(tasks) : null;
    }

    public void setTasks(List<PlayUrlTaskInfo> tasks) {
        this.tasks = tasks != null ? new ArrayList<>(tasks) : null;
    }
}
