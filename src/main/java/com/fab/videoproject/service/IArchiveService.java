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

    /**
     * Persist generated m3u8 slice information.
     *
     * @param taskId       upload task id
     * @param qualityLevel quality level of slice
     * @param fileName     m3u8 file name
     * @param filePath     absolute m3u8 file path
     * @param playUrl      play url relative to NFS root
     * @param fileSize     file size
     * @param fileMd5      md5 checksum
     * @return persisted archive record
     */
    VideoArchiveFile saveM3u8(Long taskId, String qualityLevel, String fileName,
                              String filePath, String playUrl, long fileSize,
                              String fileMd5);
}
