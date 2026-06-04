package com.example.video_convert_platform.domain.repository;

import com.example.video_convert_platform.domain.VideoArchiveFile;

import java.util.List;
import java.util.Optional;

/**
 * 视频归档文件仓储接口
 * 定义领域层对归档文件持久化的抽象
 */
public interface VideoArchiveFileRepository {

    /**
     * 保存归档文件
     * @param archiveFile 归档文件
     * @return 保存后的归档文件
     */
    VideoArchiveFile save(VideoArchiveFile archiveFile);

    /**
     * 根据ID查找归档文件
     * @param id 文件ID
     * @return 归档文件（可能为空）
     */
    Optional<VideoArchiveFile> findById(Long id);

    /**
     * 根据任务ID查找所有归档文件
     * @param taskId 任务ID
     * @return 归档文件列表
     */
    List<VideoArchiveFile> findByTaskId(Long taskId);

    /**
     * 根据任务ID和文件类型查找归档文件
     * @param taskId 任务ID
     * @param fileType 文件类型
     * @return 归档文件列表
     */
    List<VideoArchiveFile> findByTaskIdAndFileType(Long taskId, String fileType);

    /**
     * 根据任务ID、文件类型和质量级别查找归档文件
     * @param taskId 任务ID
     * @param fileType 文件类型
     * @param qualityLevel 质量级别
     * @return 归档文件（可能为空）
     */
    Optional<VideoArchiveFile> findByTaskIdAndFileTypeAndQuality(Long taskId, String fileType, String qualityLevel);

    /**
     * 删除归档文件
     * @param id 文件ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 批量删除指定任务的所有归档文件
     * @param taskId 任务ID
     * @return 删除的文件数量
     */
    int deleteByTaskId(Long taskId);
}
