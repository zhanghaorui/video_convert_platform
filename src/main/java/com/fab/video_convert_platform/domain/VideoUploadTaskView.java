package com.fab.video_convert_platform.domain;

import com.fab.video_convert_platform.domain.enums.TaskSource;
import com.fab.video_convert_platform.domain.enums.TaskStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * VideoUploadTask的不可变视图
 * 用于防御性编程，避免在事件传递过程中意外修改原始实体
 *
 * @author zhanghaorui
 */
@Getter
public final class VideoUploadTaskView {

    private final Long id;
    private final String projectNo;
    private final String patientCode;
    private final String tpStage;
    private final String uuid;
    private final Integer versionNo;
    private final String source;
    private final String status;
    private final String mainFileName;
    private final String mainFilePath;
    private final Long fileSize;
    private final String fileMd5;
    private final String errorMsg;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    /**
     * 私有构造函数，只能通过静态工厂方法创建
     */
    private VideoUploadTaskView(Long id, String projectNo, String patientCode, String tpStage,
                               String uuid, Integer versionNo, String source, String status,
                               String mainFileName, String mainFilePath, Long fileSize,
                               String fileMd5, String errorMsg, LocalDateTime createTime,
                               LocalDateTime updateTime) {
        this.id = id;
        this.projectNo = projectNo;
        this.patientCode = patientCode;
        this.tpStage = tpStage;
        this.uuid = uuid;
        this.versionNo = versionNo;
        this.source = source;
        this.status = status;
        this.mainFileName = mainFileName;
        this.mainFilePath = mainFilePath;
        this.fileSize = fileSize;
        this.fileMd5 = fileMd5;
        this.errorMsg = errorMsg;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 从VideoUploadTask创建不可变视图
     *
     * @param task 原始任务实体
     * @return 不可变视图
     */
    public static VideoUploadTaskView of(VideoUploadTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        return new VideoUploadTaskView(
            task.getId(),
            task.getProjectNo(),
            task.getPatientCode(),
            task.getTpStage(),
            task.getUuid(),
            task.getVersionNo(),
            task.getSource(),
            task.getStatus(),
            task.getMainFileName(),
            task.getMainFilePath(),
            task.getFileSize(),
            task.getFileMd5(),
            task.getErrorMsg(),
            task.getCreateTime(),
            task.getUpdateTime()
        );
    }

    /**
     * 获取任务状态枚举
     */
    public TaskStatus getTaskStatus() {
        return TaskStatus.valueOf(this.status);
    }

    /**
     * 判断任务是否已完成
     */
    public boolean isFinished() {
        return TaskStatus.FINISHED.name().equals(this.status);
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed() {
        return TaskStatus.FAILED.name().equals(this.status);
    }

    /**
     * 判断任务是否正在处理中
     */
    public boolean isProcessing() {
        return TaskStatus.PROCESSING.name().equals(this.status);
    }

    /**
     * 获取任务来源枚举
     */
    public TaskSource getTaskSource() {
        return TaskSource.fromValue(this.source);
    }

    /**
     * 判断是否为HTTP来源任务
     */
    public boolean isHttpSource() {
        try {
            return getTaskSource().isHttp();
        } catch (IllegalArgumentException e) {
            // 如果无法识别来源，默认为HTTP（向后兼容）
            return true;
        }
    }

    /**
     * 判断是否为MQ来源任务
     */
    public boolean isMqSource() {
        try {
            return getTaskSource().isMq();
        } catch (IllegalArgumentException e) {
            // 如果无法识别来源，默认不是MQ来源
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VideoUploadTaskView that = (VideoUploadTaskView) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(projectNo, that.projectNo) &&
               Objects.equals(patientCode, that.patientCode) &&
               Objects.equals(tpStage, that.tpStage) &&
               Objects.equals(uuid, that.uuid) &&
               Objects.equals(versionNo, that.versionNo) &&
               Objects.equals(source, that.source) &&
               Objects.equals(status, that.status) &&
               Objects.equals(mainFileName, that.mainFileName) &&
               Objects.equals(mainFilePath, that.mainFilePath) &&
               Objects.equals(fileSize, that.fileSize) &&
               Objects.equals(fileMd5, that.fileMd5) &&
               Objects.equals(errorMsg, that.errorMsg) &&
               Objects.equals(createTime, that.createTime) &&
               Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectNo, patientCode, tpStage, uuid, versionNo, source,
                           status, mainFileName, mainFilePath, fileSize, fileMd5, errorMsg,
                           createTime, updateTime);
    }

    @Override
    public String toString() {
        return "VideoUploadTaskView{" +
               "id=" + id +
               ", projectNo='" + projectNo + '\'' +
               ", patientCode='" + patientCode + '\'' +
               ", tpStage='" + tpStage + '\'' +
               ", uuid='" + uuid + '\'' +
               ", versionNo=" + versionNo +
               ", source='" + source + '\'' +
               ", status='" + status + '\'' +
               ", mainFileName='" + mainFileName + '\'' +
               ", mainFilePath='" + mainFilePath + '\'' +
               ", fileSize=" + fileSize +
               ", fileMd5='" + fileMd5 + '\'' +
               ", errorMsg='" + errorMsg + '\'' +
               ", createTime=" + createTime +
               ", updateTime=" + updateTime +
               '}';
    }
}
