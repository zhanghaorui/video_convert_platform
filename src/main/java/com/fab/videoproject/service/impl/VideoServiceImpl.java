package com.fab.videoproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fab.videoproject.common.BusinessException;
import com.fab.videoproject.common.VideoConstants;
import com.fab.videoproject.domain.ProjectConfig;
import com.fab.videoproject.domain.VideoUploadTask;
import com.fab.videoproject.mapper.ProjectConfigMapper;
import com.fab.videoproject.mapper.VideoUploadTaskMapper;
import com.fab.videoproject.service.IArchiveService;
import com.fab.videoproject.service.IVideoService;
import com.fab.videoproject.infra.NfsService;
import com.fab.videoproject.util.ArchivePathUtil;
import com.fab.videoproject.util.DigestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Implementation of video service operations.
 */
@Service
public class VideoServiceImpl implements IVideoService {

    @Autowired
    private ProjectConfigMapper projectConfigMapper;
    @Autowired
    private VideoUploadTaskMapper uploadTaskMapper;
    @Autowired
    private IArchiveService archiveService;
    @Autowired
    private NfsService nfsService;

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public VideoUploadTask upload(MultipartFile file, String projectNo,
                                  String patientCode, String tpStage) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw BusinessException.error("Project config not found");
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        int versionNo = VideoConstants.DEFAULT_VERSION_NO;
        String fileName = file.getOriginalFilename();
        Path path = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                projectNo, patientCode, tpStage, versionNo, uuid, fileName);
        try {
            nfsService.saveFile(file, path);
            String md5 = DigestUtil.md5(path);
            VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                    tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, fileName,
                    path.toString(), file.getSize(), md5);
            uploadTaskMapper.insert(task);
            archiveService.saveOriginal(task.getId(), fileName, path.toString(),
                    file.getSize(), md5);
            return uploadTaskMapper.selectById(task.getId());
        } catch (IOException e) {
            throw BusinessException.error("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void uploadChunk(MultipartFile file, Integer chunk, Integer chunks,
                            String filename, String projectNo, String patientCode,
                            String tpStage, String uuid) {
        ProjectConfig config = projectConfigMapper.selectOne(
                new QueryWrapper<ProjectConfig>().eq("project_no", projectNo).last("limit 1"));
        if (config == null) {
            throw BusinessException.error("Project config not found");
        }

        int versionNo = VideoConstants.DEFAULT_VERSION_NO;
        Path chunkDir = ArchivePathUtil.buildChunkPath(config.getArchiveRoot(),
                projectNo, patientCode, tpStage, versionNo, uuid);
        try {
            nfsService.saveChunk(file, chunkDir, chunk == null ? 0 : chunk);
            if (chunk != null && chunks != null && chunk + 1 == chunks) {
                Path target = ArchivePathUtil.buildOriginalPath(config.getArchiveRoot(),
                        projectNo, patientCode, tpStage, versionNo, uuid, filename);
                nfsService.mergeChunks(chunkDir, target, chunks);
                long size = Files.size(target);
                String md5 = DigestUtil.md5(target);
                VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                        tpStage, uuid, versionNo, VideoConstants.SOURCE_CONTROLLER, filename,
                        target.toString(), size, md5);
                uploadTaskMapper.insert(task);
                archiveService.saveOriginal(task.getId(), filename, target.toString(),
                        size, md5);
                nfsService.deleteRecursively(chunkDir);
            }
        } catch (IOException e) {
            throw BusinessException.error("Failed to store chunk: " + e.getMessage());
        }
    }
}
