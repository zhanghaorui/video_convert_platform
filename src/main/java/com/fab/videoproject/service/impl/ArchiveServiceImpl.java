package com.fab.videoproject.service.impl;

import com.fab.videoproject.common.VideoConstants;
import com.fab.videoproject.domain.VideoArchiveFile;
import com.fab.videoproject.domain.enums.ArchiveStatus;
import com.fab.videoproject.mapper.VideoArchiveFileMapper;
import com.fab.videoproject.service.IArchiveService;
import com.fab.videoproject.util.DateUtil;
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
}
