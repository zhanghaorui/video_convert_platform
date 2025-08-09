package com.fab.video_convert_platform.domain.aggregate;

import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.valueobject.FileInfo;
import com.fab.video_convert_platform.domain.valueobject.PatientVisit;
import com.fab.video_convert_platform.domain.valueobject.ProjectNo;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 视频上传任务聚合根
 * 管理VideoUploadTask及其相关的VideoArchiveFile集合
 * 确保业务不变性和一致性
 */
@Getter
public class VideoUploadTaskAggregate {

    private final VideoUploadTask uploadTask;
    private final List<VideoArchiveFile> archiveFiles;

    /**
     * 私有构造函数，强制使用工厂方法
     */
    private VideoUploadTaskAggregate(VideoUploadTask uploadTask) {
        this.uploadTask = uploadTask;
        this.archiveFiles = new ArrayList<>();
    }

    /**
     * 创建新的视频上传任务聚合根
     * 
     * @param projectNo 项目编号值对象
     * @param patientVisit 患者访视信息值对象
     * @param uuid 唯一标识
     * @param versionNo 版本号
     * @param source 来源
     * @param fileInfo 文件信息值对象
     * @param filePath 文件路径
     * @return 聚合根实例
     */
    public static VideoUploadTaskAggregate create(ProjectNo projectNo, PatientVisit patientVisit,
                                                 String uuid, Integer versionNo, String source,
                                                 FileInfo fileInfo, String filePath) {
        
        VideoUploadTask task = VideoUploadTask.createOriginalSaved(
            projectNo.getValue(),
            patientVisit.getPatientCode(),
            patientVisit.getTpStage(),
            uuid,
            versionNo,
            source,
            fileInfo.getFileName(),
            filePath,
            fileInfo.getFileSize(),
            fileInfo.getFileMd5()
        );

        return new VideoUploadTaskAggregate(task);
    }

    /**
     * 从现有任务重建聚合根（用于从数据库加载）
     * 
     * @param task 上传任务
     * @param archiveFiles 归档文件列表
     * @return 聚合根实例
     */
    public static VideoUploadTaskAggregate rebuild(VideoUploadTask task, List<VideoArchiveFile> archiveFiles) {
        VideoUploadTaskAggregate aggregate = new VideoUploadTaskAggregate(task);
        aggregate.archiveFiles.addAll(archiveFiles != null ? archiveFiles : Collections.emptyList());
        return aggregate;
    }

    /**
     * 添加归档文件
     * 业务规则：只有ORIGINAL_SAVED或PROCESSING状态的任务才能添加归档文件
     * 
     * @param fileType 文件类型
     * @param qualityLevel 质量级别
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param playUrl 播放URL
     * @param fileSize 文件大小
     * @param fileMd5 文件MD5
     */
    public void addArchiveFile(String fileType, String qualityLevel, String fileName,
                              String filePath, String playUrl, Long fileSize, String fileMd5) {
        
        if (uploadTask.isFailed()) {
            throw new IllegalStateException("失败的任务不能添加归档文件");
        }

        VideoArchiveFile archiveFile = VideoArchiveFile.create(
            uploadTask.getId(),
            fileType,
            qualityLevel,
            fileName,
            filePath,
            playUrl,
            fileSize,
            fileMd5
        );

        this.archiveFiles.add(archiveFile);
    }

    /**
     * 获取指定类型和质量的归档文件
     * 
     * @param fileType 文件类型
     * @param qualityLevel 质量级别
     * @return 归档文件（可能为null）
     */
    public VideoArchiveFile getArchiveFile(String fileType, String qualityLevel) {
        return archiveFiles.stream()
            .filter(file -> fileType.equals(file.getFileType()) && 
                           qualityLevel.equals(file.getQualityLevel()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有归档文件（只读）
     * 
     * @return 归档文件列表（不可修改）
     */
    public List<VideoArchiveFile> getArchiveFiles() {
        return Collections.unmodifiableList(archiveFiles);
    }

    /**
     * 获取指定类型的所有归档文件
     * 
     * @param fileType 文件类型
     * @return 归档文件列表
     */
    public List<VideoArchiveFile> getArchiveFilesByType(String fileType) {
        return archiveFiles.stream()
            .filter(file -> fileType.equals(file.getFileType()))
            .collect(ArrayList::new, (list, file) -> list.add(file), (list1, list2) -> list1.addAll(list2));
    }

    /**
     * 标记任务为处理中
     * 业务规则：只有ORIGINAL_SAVED状态的任务才能标记为处理中
     */
    public void markProcessing() {
        uploadTask.markProcessing();
    }

    /**
     * 标记任务为完成
     * 业务规则：只有PROCESSING状态的任务才能标记为完成
     */
    public void markFinished() {
        uploadTask.markFinished();
    }

    /**
     * 标记任务为失败
     * 
     * @param errorMessage 错误信息
     */
    public void markError(String errorMessage) {
        uploadTask.markError(errorMessage);
    }

    /**
     * 检查任务是否有足够的归档文件
     * 业务规则：至少需要一个ORIGINAL类型和一个M3U8类型的文件
     * 
     * @return 是否有足够的归档文件
     */
    public boolean hasCompleteArchiveFiles() {
        boolean hasOriginal = archiveFiles.stream()
            .anyMatch(file -> "ORIGINAL".equals(file.getFileType()));
        
        boolean hasM3u8 = archiveFiles.stream()
            .anyMatch(file -> "M3U8".equals(file.getFileType()));
        
        return hasOriginal && hasM3u8;
    }

    /**
     * 获取任务的文件信息值对象
     * 
     * @return 文件信息值对象
     */
    public FileInfo getFileInfo() {
        return FileInfo.of(
            uploadTask.getMainFileName(),
            uploadTask.getFileSize(),
            uploadTask.getFileMd5()
        );
    }

    /**
     * 获取项目编号值对象
     * 
     * @return 项目编号值对象
     */
    public ProjectNo getProjectNo() {
        return ProjectNo.of(uploadTask.getProjectNo());
    }

    /**
     * 获取患者访视信息值对象
     * 
     * @return 患者访视信息值对象
     */
    public PatientVisit getPatientVisit() {
        return PatientVisit.of(uploadTask.getPatientCode(), uploadTask.getTpStage());
    }
}
