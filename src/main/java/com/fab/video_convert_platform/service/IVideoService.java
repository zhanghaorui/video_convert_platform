package com.fab.video_convert_platform.service;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.dto.MqVideoMessage;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for video-related operations.
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
}
