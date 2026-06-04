package com.example.video_convert_platform.service.dto;

import lombok.Data;

/**
 * Payload for MQ-driven video tasks.
 */
@Data
public class MqVideoMessage {

    private String projectNo;
    private String patientCode;
    private String tpStage;
    /** 可选：访视描述（与 tpStage 并存，仅作为附加元数据存储，不参与路径构建） */
    private String visit; // 新增字段
    /** 检查日期 */
    private String checkDate; // 新增字段
    /** Absolute path to original video file. */
    private String filePath;
    /** MD5 checksum provided by upstream. 可选，补推消息可能为空 */
    private String fileMd5;
}
