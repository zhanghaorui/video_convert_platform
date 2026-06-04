package com.example.video_convert_platform.interfaces.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频上传任务响应DTO
 *
 */
@Data
public class VideoUploadResponse {

    /**
     * 任务ID
     */
    private Long taskId;

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
     * 唯一标识
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
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 错误信息（如果有）
     */
    private String errorMsg;

    /**
     * 访视描述
     */
    private String visit;

    /**
     * 检查日期
     */
    private String checkDate;
}
