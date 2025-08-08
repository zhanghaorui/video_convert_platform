package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.domain.enums.ArchiveStatus;
import com.fab.video_convert_platform.mapper.VideoArchiveFileMapper;
import com.fab.video_convert_platform.service.IArchiveService;
import com.fab.video_convert_platform.util.DateUtil;
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
        VideoArchiveFile archive = new VideoArchiveFile();
        archive.setTaskId(taskId);
        archive.setFileType(VideoConstants.FILE_TYPE_ORIGINAL);
        archive.setQualityLevel(VideoConstants.QUALITY_ORIGINAL);
        archive.setFileName(fileName);
        archive.setFilePath(filePath);
        archive.setFileSize(fileSize);
        archive.setFileMd5(fileMd5);
        archive.setStatus(ArchiveStatus.SAVED.name());
        archive.setCreateTime(DateUtil.now());
        archiveFileMapper.insert(archive);
        return archive;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = RuntimeException.class)
    public VideoArchiveFile saveM3u8(Long taskId, String qualityLevel, String fileName,
                                     String filePath, String playUrl, long fileSize,
                                     String fileMd5) {
        VideoArchiveFile archive = new VideoArchiveFile();
        archive.setTaskId(taskId);
        archive.setFileType(VideoConstants.FILE_TYPE_M3U8);
        archive.setQualityLevel(qualityLevel);
        archive.setFileName(fileName);
        archive.setFilePath(filePath);
        archive.setPlayUrl(playUrl);
        archive.setFileSize(fileSize);
        archive.setFileMd5(fileMd5);
        archive.setStatus(ArchiveStatus.READY.name());
        archive.setCreateTime(DateUtil.now());
        archiveFileMapper.insert(archive);
        return archive;
    }
}
