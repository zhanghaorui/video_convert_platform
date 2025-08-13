package com.fab.video_convert_platform.service;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for video-related operations.
 * @author 张浩锐
 */
public interface IVideoService {

    /**
     * Upload full video and archive original file.
     *
     * @param file        video file
     * @param projectNo   project number
     * @param patientCode subject code
     * @param tpStage     visit stage
     * @return persisted upload task
     */
    VideoUploadTask upload(MultipartFile file, String projectNo,
                           String patientCode, String tpStage);

    /**
     * Upload video in chunks. When the last chunk arrives, merge and archive.
     *
     * @param file        current chunk data
     * @param chunk       current chunk index
     * @param chunks      total chunk count
     * @param filename    original file name
     * @param projectNo   project number
     * @param patientCode subject code
     * @param tpStage     visit stage
     * @param uuid        unique id for upload session
     */
    void uploadChunk(MultipartFile file, Integer chunk, Integer chunks,
                     String filename, String projectNo, String patientCode,
                     String tpStage, String uuid);

    /**
     * Handle tasks pushed via MQ.
     *
     * @param message MQ message payload
     */
    void processMqMessage(MqVideoMessage message);

    /**
     * Process video task from MQ message with MQ source
     *
     * @param message MQ message payload
     */
    void processFromMq(MqVideoMessage message);

    /**
     * Get upload task information by task ID.
     *
     * @param taskId task ID
     * @return upload task information
     * @throws com.fab.video_convert_platform.common.BusinessException if the task does not exist
     */
    VideoUploadTask getTaskById(Long taskId);

    /**
     * Get play URLs by task ID.
     *
     * @param taskId task ID
     * @return list of archive files with play URLs
     * @throws com.fab.video_convert_platform.common.BusinessException if the task does not exist
     */
    List<VideoArchiveFile> getPlayUrlsByTaskId(Long taskId);

    /**
     * Get play URLs by business parameters.
     *
     * @param projectNo   project number
     * @param patientCode patient code
     * @param tpStage     visit stage
     * @param versionNo   version number (optional, if null returns latest version)
     * @param quality     quality level (optional, if null returns all qualities)
     * @return list of archive files with play URLs
     */
    List<VideoArchiveFile> getPlayUrlsByParams(String projectNo, 
                                               String patientCode, 
                                               String tpStage, 
                                               Integer versionNo, 
                                               String quality);
}
