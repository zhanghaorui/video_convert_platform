package com.example.video_convert_platform.infra.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Persistence object representing the video_upload_task table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_upload_task")
public class VideoUploadTaskPO extends BasePO {

    @TableField("project_no")
    private String projectNo;

    @TableField("patient_code")
    private String patientCode;

    @TableField("tp_stage")
    private String tpStage;

    @TableField("visit") // 新增访视描述字段
    private String visit;

    @TableField("check_date") // 新增检查日期字段
    private String checkDate;

    @TableField("uuid")
    private String uuid;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("source")
    private String source;

    @TableField("status")
    private String status;

    @TableField("main_file_name")
    private String mainFileName;

    @TableField("main_file_path")
    private String mainFilePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("error_msg")
    private String errorMsg;
}
