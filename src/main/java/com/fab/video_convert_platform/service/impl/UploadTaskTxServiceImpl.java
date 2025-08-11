package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.service.IArchiveService;
import com.fab.video_convert_platform.service.IUploadTaskTxService;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * Implements transactional persistence of upload tasks and archive records.
 */
@Service
public class UploadTaskTxServiceImpl implements IUploadTaskTxService {

    private final VideoUploadTaskRepository uploadTaskRepository;
    private final IArchiveService archiveService;
    private final Tracer tracer;

    public UploadTaskTxServiceImpl(VideoUploadTaskRepository uploadTaskRepository,
                                   IArchiveService archiveService,
                                   Tracer tracer) {
        this.uploadTaskRepository = uploadTaskRepository;
        this.archiveService = archiveService;
        this.tracer = tracer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoUploadTask saveUploadTaskInTransaction(String projectNo, String patientCode,
            String tpStage, String uuid, Integer versionNo, String source,
            String fileName, Path filePath, Long fileSize, String md5) {

        Span span = tracer.nextSpan().name("task_db_upsert").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                    tpStage, uuid, versionNo, source, fileName,
                    filePath.toString(), fileSize, md5);

            uploadTaskRepository.save(task);

            span.tag("task_id", String.valueOf(task.getId()));
            span.tag("status", task.getStatus());

            String fileName, String filePath, Long fileSize, String md5) {

        Span span = tracer.nextSpan().name("task_db_upsert").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            VideoUploadTask task = VideoUploadTask.createOriginalSaved(projectNo, patientCode,
                    tpStage, uuid, versionNo, source, fileName,
                    filePath, fileSize, md5);

            uploadTaskRepository.save(task);

            span.tag("task_id", String.valueOf(task.getId()));
            span.tag("status", task.getStatus());

            archiveService.saveOriginal(task.getId(), fileName, filePath,
                    fileSize, md5);

            return task;
        } finally {
            span.end();
        }
    }
}
