package com.example.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.video_convert_platform.domain.enums.ArchiveStatus;
import com.example.video_convert_platform.util.DateUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 视频归档文件领域实体
 * 遵循DDD设计原则，封装归档文件相关的业务逻辑
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@TableName("video_archive_file")
public class VideoArchiveFile extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("file_type")
    private String fileType;

    @TableField("quality_level")
    private String qualityLevel;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("play_url")
    private String playUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_md5")
    private String fileMd5;

    /**
     * Processing status, see {@link ArchiveStatus}.
     */
    @TableField("status")
    private String status;

    @TableField("remark")
    private String remark;

    /**
     * 无参构造函数，供MyBatis-Plus使用
     */
    public VideoArchiveFile() {
    }

    /**
     * 创建新的归档文件记录
     * 
     * @param taskId 任务ID
     * @param fileType 文件类型
     * @param qualityLevel 质量级别
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param playUrl 播放URL
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     * @return 归档文件实体
     */
    public static VideoArchiveFile create(Long taskId, String fileType, String qualityLevel,
                                        String fileName, String filePath, String playUrl,
                                        Long fileSize, String fileMd5) {
        return create(taskId, fileType, qualityLevel, fileName, filePath, playUrl, fileSize, fileMd5, ArchiveStatus.ACTIVE);
    }

    /**
     * 创建新的归档文件记录，指定初始状态
     * 
     * @param taskId 任务ID
     * @param fileType 文件类型
     * @param qualityLevel 质量级别
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param playUrl 播放URL
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     * @param status 初始状态
     * @return 归档文件实体
     */
    public static VideoArchiveFile create(Long taskId, String fileType, String qualityLevel,
                                        String fileName, String filePath, String playUrl,
                                        Long fileSize, String fileMd5, ArchiveStatus status) {
        validateCreateParams(taskId, fileType, fileName, filePath, fileSize, fileMd5);
        
        VideoArchiveFile archiveFile = new VideoArchiveFile();
        archiveFile.taskId = taskId;
        archiveFile.fileType = fileType;
        archiveFile.qualityLevel = qualityLevel;
        archiveFile.fileName = fileName;
        archiveFile.filePath = filePath;
        archiveFile.playUrl = playUrl;
        archiveFile.fileSize = fileSize;
        archiveFile.fileMd5 = fileMd5;
        archiveFile.status = status.name();
        
        LocalDateTime now = DateUtil.now();
        archiveFile.setCreateTime(now);
        archiveFile.setUpdateTime(now);
        
        return archiveFile;
    }

    /**
     * 判断文件是否处于活跃状态
     * 
     * @return true if active
     */
    public boolean isActive() {
        return ArchiveStatus.ACTIVE.name().equals(this.status);
    }

    /**
     * 判断文件是否已归档
     * 
     * @return true if archived
     */
    public boolean isArchived() {
        return ArchiveStatus.ARCHIVED.name().equals(this.status);
    }

    /**
     * 判断文件是否已删除
     * 
     * @return true if deleted
     */
    public boolean isDeleted() {
        return ArchiveStatus.DELETED.name().equals(this.status);
    }

    /**
     * 标记文件为已归档状态
     * 业务规则：只有活跃状态的文件才能被标记为已归档
     */
    public void markAsArchived() {
        if (!isActive()) {
            throw new IllegalStateException("只有活跃状态的文件才能被标记为已归档，当前状态: " + this.status);
        }
        
        this.status = ArchiveStatus.ARCHIVED.name();
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 标记文件为已删除状态
     * 业务规则：活跃和已归档的文件都可以被删除
     */
    public void markAsDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("文件已处于删除状态");
        }
        
        this.status = ArchiveStatus.DELETED.name();
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 更新播放URL
     * 
     * @param playUrl 新的播放URL
     */
    public void updatePlayUrl(String playUrl) {
        this.playUrl = playUrl;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 更新备注信息
     * 
     * @param remark 备注信息
     */
    public void updateRemark(String remark) {
        this.remark = remark;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 获取归档状态枚举
     * 
     * @return 归档状态枚举
     */
    public ArchiveStatus getArchiveStatus() {
        return ArchiveStatus.valueOf(this.status);
    }

    /**
     * 校验创建参数
     */
    private static void validateCreateParams(Long taskId, String fileType, String fileName, 
                                           String filePath, Long fileSize, String fileMd5) {
        if (Objects.isNull(taskId) || taskId <= 0) {
            throw new IllegalArgumentException("任务ID必须大于0");
        }
        if (!StringUtils.hasText(fileType)) {
            throw new IllegalArgumentException("文件类型不能为空");
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

