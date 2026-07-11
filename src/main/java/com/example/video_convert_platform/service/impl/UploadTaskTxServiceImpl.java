package com.example.video_convert_platform.service.impl;

import com.example.video_convert_platform.domain.VideoUploadTask;
import com.example.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.example.video_convert_platform.service.ArchiveService;
import com.example.video_convert_platform.service.UploadTaskTxService;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * Implements transactional persistence of upload tasks and archive records.
 */
@Service
public class UploadTaskTxServiceImpl implements UploadTaskTxService {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final VideoUploadTaskRepository uploadTaskRepository;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ArchiveService archiveService;
    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final Tracer tracer;

    public UploadTaskTxServiceImpl(VideoUploadTaskRepository uploadTaskRepository,
                                   ArchiveService archiveService,
                                   Tracer tracer) {
        this.uploadTaskRepository = uploadTaskRepository;
        this.archiveService = archiveService;
        this.tracer = tracer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadTask saveUploadTaskInTransaction(String projectNo, String patientCode,
            String tpStage, String uuid, Integer versionNo, String source,
            String fileName, Path filePath, Long fileSize, String md5, String visit, String checkDate) {

        Span span = tracer.nextSpan().name("task_db_upsert").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                    tpStage, uuid, versionNo, source, fileName,
                    filePath.toString(), fileSize, md5, visit, checkDate);

            uploadTaskRepository.save(task);

            span.tag("task_id", String.valueOf(task.getId()));
            span.tag("status", task.getStatus());

            archiveService.saveOriginal(task.getId(), fileName, filePath.toString(),
                    fileSize, md5);

            return task;
        } finally {
            span.end();
        }
    }
}
