package com.fab.videoproject.service;

import com.fab.videoproject.domain.VideoArchiveFile;

/**
 * Interface for archive operations.
 */
public interface IArchiveService {

    /**
     * Persist original uploaded file information.
     *
     * @param taskId   upload task id
     * @param fileName file name
     * @param filePath absolute file path
     * @param fileSize file size
     * @param fileMd5  md5 checksum
     * @return persisted archive record
     */
    VideoArchiveFile saveOriginal(Long taskId, String fileName, String filePath,
                                  long fileSize, String fileMd5);
}
