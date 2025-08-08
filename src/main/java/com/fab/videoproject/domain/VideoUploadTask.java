package com.fab.videoproject.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fab.videoproject.domain.enums.TaskStatus;
import com.fab.videoproject.util.DateUtil;
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

    /**
     * Task processing status, see {@link com.fab.videoproject.domain.enums.TaskStatus}.
     */
    private String status;

    private String mainFileName;

    private String mainFilePath;

    private Long fileSize;

    private String fileMd5;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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
}
