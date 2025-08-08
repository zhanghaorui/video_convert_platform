package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fab.video_convert_platform.domain.enums.TaskStatus;
import com.fab.video_convert_platform.util.DateUtil;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Upload task entity mapped to video_upload_task table.
 */
@Data
@TableName("video_upload_task")
public class VideoUploadTask extends BaseEntity {

    @TableField("project_no")
    private String projectNo;

    @TableField("patient_code")
    private String patientCode;

    @TableField("tp_stage")
    private String tpStage;

    @TableField("uuid")
    private String uuid;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("source")
    private String source;

    /**
     * Task processing status, see {@link com.fab.video_convert_platform.domain.enums.TaskStatus}.
     */
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

    /**
     * Build a task for an original video that has been saved to NFS.
     */
    public static VideoUploadTask createOriginalSaved(String projectNo, String patientCode,
                                                      String tpStage, String uuid, Integer versionNo,
                                                      String source, String fileName, String filePath,
                                                      long fileSize, String fileMd5) {
        VideoUploadTask task = new VideoUploadTask();
        task.setProjectNo(projectNo);
        task.setPatientCode(patientCode);
        task.setTpStage(tpStage);
        task.setUuid(uuid);
        task.setVersionNo(versionNo);
        task.setSource(source);
        task.setStatus(TaskStatus.ORIGINAL_SAVED.name());
        task.setMainFileName(fileName);
        task.setMainFilePath(filePath);
        task.setFileSize(fileSize);
        task.setFileMd5(fileMd5);
        LocalDateTime now = DateUtil.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    /** Mark task as finished. */
    public void markFinished() {
        this.status = TaskStatus.FINISHED.name();
        this.updateTime = DateUtil.now();
    }

    /** Mark task as failed with error message. */
    public void markError(String error) {
        this.status = TaskStatus.FAILED.name();
        this.errorMsg = error;
        this.updateTime = DateUtil.now();
    }
}
