package com.example.video_convert_platform.service;

import com.example.video_convert_platform.domain.VideoUploadTask;

import java.nio.file.Path;

/**
 * Transactional service for persisting upload tasks and related archives.
 */
public interface UploadTaskTxService {

    /**
     * Persist upload task and archive record within a transaction.
     *
     * @return saved {@link VideoUploadTask}
     */
    VideoUploadTask saveUploadTaskInTransaction(String projectNo, String patientCode,
            String tpStage, String uuid, Integer versionNo, String source,
            String fileName, Path filePath, Long fileSize, String md5, String visit, String checkDate);
}
