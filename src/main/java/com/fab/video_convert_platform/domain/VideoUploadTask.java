package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fab.video_convert_platform.domain.enums.TaskStatus;
import com.fab.video_convert_platform.util.DateUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 视频上传任务领域实体
 * 遵循DDD设计原则，封装业务逻辑和状态变更
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
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
     * 任务处理状态
     * @see TaskStatus
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
     * 私有构造函数，强制使用工厂方法创建实例
     */
    private VideoUploadTask() {
    }

    /**
     * 创建原始视频已保存状态的任务实体
     *
     * @param projectNo   项目编号
     * @param patientCode 受试者编码
     * @param tpStage     访视点
     * @param uuid        唯一标识
     * @param versionNo   版本号
     * @param source      来源
     * @param fileName    文件名
     * @param filePath    文件路径
     * @param fileSize    文件大小
     * @param fileMd5     文件MD5
     * @return 视频上传任务实体
     */
    public static VideoUploadTask createOriginalSaved(String projectNo, String patientCode,
                                                      String tpStage, String uuid, Integer versionNo,
                                                      String source, String fileName, String filePath,
                                                      Long fileSize, String fileMd5) {
        // 参数校验
        validateCreateParams(projectNo, patientCode, tpStage, uuid, versionNo,
            source, fileName, filePath, fileSize, fileMd5);

        VideoUploadTask task = new VideoUploadTask();
        task.projectNo = projectNo;
        task.patientCode = patientCode;
        task.tpStage = tpStage;
        task.uuid = uuid;
        task.versionNo = versionNo;
        task.source = source;
        task.status = TaskStatus.ORIGINAL_SAVED.name();
        task.mainFileName = fileName;
        task.mainFilePath = filePath;
        task.fileSize = fileSize;
        task.fileMd5 = fileMd5;

        LocalDateTime now = DateUtil.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);

        return task;
    }

    /**
     * 标记任务为完成状态
     * 业务规则：只有处理中的任务才能标记为完成
     */
    public void markFinished() {
        if (!canTransitionToFinished()) {
            throw new IllegalStateException("任务状态不允许直接标记为完成，当前状态: " + this.status);
        }

        this.status = TaskStatus.FINISHED.name();
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 标记任务为失败状态
     *
     * @param errorMessage 错误信息
     */
    public void markError(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            throw new IllegalArgumentException("错误信息不能为空");
        }

        this.status = TaskStatus.FAILED.name();
        this.errorMsg = errorMessage;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 标记任务为处理中状态
     */
    public void markProcessing() {
        if (!canTransitionToProcessing()) {
            throw new IllegalStateException("任务状态不允许标记为处理中，当前状态: " + this.status);
        }

        this.status = TaskStatus.PROCESSING.name();
        this.setUpdateTime(DateUtil.now());
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
     * 获取任务状态枚举
     */
    public TaskStatus getTaskStatus() {
        return TaskStatus.valueOf(this.status);
    }

    /**
     * 校验创建参数
     */
    private static void validateCreateParams(String projectNo, String patientCode, String tpStage,
                                           String uuid, Integer versionNo, String source,
                                           String fileName, String filePath, Long fileSize, String fileMd5) {
        if (!StringUtils.hasText(projectNo)) {
            throw new IllegalArgumentException("项目编号不能为空");
        }
        if (!StringUtils.hasText(patientCode)) {
            throw new IllegalArgumentException("受试者编码不能为空");
        }
        if (!StringUtils.hasText(tpStage)) {
            throw new IllegalArgumentException("访视点不能为空");
        }
        if (!StringUtils.hasText(uuid)) {
            throw new IllegalArgumentException("UUID不能为空");
        }
        if (Objects.isNull(versionNo) || versionNo <= 0) {
            throw new IllegalArgumentException("版本号必须大于0");
        }
        if (!StringUtils.hasText(source)) {
            throw new IllegalArgumentException("来源不能为空");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (!StringUtils.hasText(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (Objects.isNull(fileSize) || fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必须大于0");
        }
        if (!StringUtils.hasText(fileMd5)) {
            throw new IllegalArgumentException("文件MD5不能为空");
        }
    }

    /**
     * 判断是否可以转换为完成状态
     */
    private boolean canTransitionToFinished() {
        return TaskStatus.PROCESSING.name().equals(this.status) ||
               TaskStatus.ORIGINAL_SAVED.name().equals(this.status);
    }

    /**
     * 判断是否可以转换为处理中状态
     */
    private boolean canTransitionToProcessing() {
        return TaskStatus.ORIGINAL_SAVED.name().equals(this.status);
    }

    // 为MyBatis-Plus提供必要的setter方法（仅限框架使用）
    public void setId(Long id) {
        super.setId(id);
    }

    public void setCreateTime(LocalDateTime createTime) {
        super.setCreateTime(createTime);
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        super.setUpdateTime(updateTime);
    }
}
