package com.example.video_convert_platform.domain;

import com.example.video_convert_platform.domain.enums.TaskSource;
import com.example.video_convert_platform.domain.enums.TaskStatus;
import com.example.video_convert_platform.util.DateUtil;
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
public class VideoUploadTask extends BaseEntity {

    private String projectNo;

    private String patientCode;

    private String tpStage;

    private String visit; // 新增访视描述字段

    private String checkDate; // 新增检查日期字段

    private String uuid;

    private Integer versionNo;

    private String source;

    /**
     * 任务处理状态
     * @see TaskStatus
     */
    private String status;

    private String mainFileName;

    private String mainFilePath;

    private Long fileSize;

    private String fileMd5;

    private String errorMsg;

    /**
     * 无参构造函数，供反序列化或重建使用
     */
    public VideoUploadTask() {
    }

    /**
     * 从持久化对象重建领域实体
     */
    public static VideoUploadTask rebuild(Long id, String projectNo, String patientCode, String tpStage,
                                          String uuid, Integer versionNo, String source, String status,
                                          String mainFileName, String mainFilePath, Long fileSize,
                                          String fileMd5, String errorMsg, LocalDateTime createTime,
                                          LocalDateTime updateTime, String visit, String checkDate) { // 新增 visit 和 checkDate 参与重建
        VideoUploadTask task = new VideoUploadTask();
        task.setId(id);
        task.projectNo = projectNo;
        task.patientCode = patientCode;
        task.tpStage = tpStage;
        task.visit = visit;
        task.checkDate = checkDate;
        task.uuid = uuid;
        task.versionNo = versionNo;
        task.source = source;
        task.status = status;
        task.mainFileName = mainFileName;
        task.mainFilePath = mainFilePath;
        task.fileSize = fileSize;
        task.fileMd5 = fileMd5;
        task.errorMsg = errorMsg;
        task.setCreateTime(createTime);
        task.setUpdateTime(updateTime);
        return task;
    }

    // 兼容旧的rebuild签名（无visit和checkDate）
    public static VideoUploadTask rebuild(Long id, String projectNo, String patientCode, String tpStage,
                                          String uuid, Integer versionNo, String source, String status,
                                          String mainFileName, String mainFilePath, Long fileSize,
                                          String fileMd5, String errorMsg, LocalDateTime createTime,
                                          LocalDateTime updateTime, String visit) {
        return rebuild(id, projectNo, patientCode, tpStage, uuid, versionNo, source, status,
                mainFileName, mainFilePath, fileSize, fileMd5, errorMsg, createTime, updateTime, visit, null);
    }

    // 兼容旧的rebuild签名（无visit）
    public static VideoUploadTask rebuild(Long id, String projectNo, String patientCode, String tpStage,
                                          String uuid, Integer versionNo, String source, String status,
                                          String mainFileName, String mainFilePath, Long fileSize,
                                          String fileMd5, String errorMsg, LocalDateTime createTime,
                                          LocalDateTime updateTime) {
        return rebuild(id, projectNo, patientCode, tpStage, uuid, versionNo, source, status,
                mainFileName, mainFilePath, fileSize, fileMd5, errorMsg, createTime, updateTime, null, null);
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
     * @param visit       访视描述
     * @param checkDate   检查日期
     * @return 视频上传任务实体
     */
    public static VideoUploadTask createOriginalSaved(String projectNo, String patientCode,
                                                      String tpStage, String uuid, Integer versionNo,
                                                      String source, String fileName, String filePath,
                                                      Long fileSize, String fileMd5, String visit, String checkDate) { // 添加 visit 和 checkDate
        // 参数校验
        validateCreateParams(projectNo, patientCode, tpStage, uuid, versionNo,
            source, fileName, filePath, fileSize, fileMd5);

        VideoUploadTask task = new VideoUploadTask();
        task.projectNo = projectNo;
        task.patientCode = patientCode;
        task.tpStage = tpStage;
        task.visit = visit;
        task.checkDate = checkDate;
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

    // 兼容旧的createOriginalSaved签名（无checkDate）
    public static VideoUploadTask createOriginalSaved(String projectNo, String patientCode,
                                                      String tpStage, String uuid, Integer versionNo,
                                                      String source, String fileName, String filePath,
                                                      Long fileSize, String fileMd5, String visit) {
        return createOriginalSaved(projectNo, patientCode, tpStage, uuid, versionNo, source,
                fileName, filePath, fileSize, fileMd5, visit, null);
    }

    // 兼容旧的createOriginalSaved签名（无visit）
    public static VideoUploadTask createOriginalSaved(String projectNo, String patientCode,
                                                      String tpStage, String uuid, Integer versionNo,
                                                      String source, String fileName, String filePath,
                                                      Long fileSize, String fileMd5) {
        return createOriginalSaved(projectNo, patientCode, tpStage, uuid, versionNo, source,
                fileName, filePath, fileSize, fileMd5, null, null);
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
        // MQ来源时允许tpStage为空，其他来源时必填
        if (!"mq".equalsIgnoreCase(source) && !StringUtils.hasText(tpStage)) {
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
}
