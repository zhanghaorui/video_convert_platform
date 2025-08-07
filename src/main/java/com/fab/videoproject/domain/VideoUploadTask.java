package com.fab.videoproject.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Upload task entity mapped to video_upload_task table.
 */
@Data
@TableName("video_upload_task")
public class VideoUploadTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectNo;

    private String patientCode;

    private String tpStage;

    private String uuid;

    private Integer versionNo;

    private String source;

    private String status;

    private String mainFileName;

    private String mainFilePath;

    private Long fileSize;

    private String fileMd5;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
