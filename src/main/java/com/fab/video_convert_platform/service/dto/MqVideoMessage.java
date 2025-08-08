package com.fab.video_convert_platform.service.dto;

import lombok.Data;

/**
 * Payload for MQ-driven video tasks.
 */
@Data
public class MqVideoMessage {

    private String projectNo;
    private String patientCode;
    private String tpStage;
    /** Absolute path to original video file. */
    private String filePath;
    /** MD5 checksum provided by upstream. */
    private String fileMd5;
}

