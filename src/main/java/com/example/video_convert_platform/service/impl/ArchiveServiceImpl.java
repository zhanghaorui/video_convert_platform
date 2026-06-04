package com.example.video_convert_platform.service.impl;

import com.example.video_convert_platform.common.VideoConstants;
import com.example.video_convert_platform.domain.VideoArchiveFile;
import com.example.video_convert_platform.mapper.VideoArchiveFileMapper;
import com.example.video_convert_platform.service.IArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * Implementation of archive service.
 */
@Service
public class ArchiveServiceImpl implements IArchiveService {

    @Autowired
    private VideoArchiveFileMapper archiveFileMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = RuntimeException.class)
    public VideoArchiveFile saveOriginal(Long taskId, String fileName, String filePath,
                                         long fileSize, String fileMd5) {
        VideoArchiveFile archive = VideoArchiveFile.create(
            taskId, 
            VideoConstants.FILE_TYPE_ORIGINAL,
            VideoConstants.QUALITY_ORIGINAL,
            fileName,
            filePath,
            null,
            fileSize,
            fileMd5,
            com.example.video_convert_platform.domain.enums.ArchiveStatus.SAVED
        );
        archiveFileMapper.insert(archive);
        return archive;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public VideoArchiveFile saveM3u8(Long taskId, String qualityLevel, String fileName,
                                     String filePath, String playUrl, long fileSize,
                                     String fileMd5) {
        VideoArchiveFile archive = VideoArchiveFile.create(
            taskId,
            VideoConstants.FILE_TYPE_M3U8,
            qualityLevel,
            fileName,
            filePath,
            playUrl,
            fileSize,
            fileMd5,
            com.example.video_convert_platform.domain.enums.ArchiveStatus.READY
        );
        archiveFileMapper.insert(archive);
        return archive;
    }
}
