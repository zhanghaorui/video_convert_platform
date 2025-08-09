package com.fab.video_convert_platform.infra;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.service.VideoTaskDomainService;
import com.fab.video_convert_platform.service.ITaskLogService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Local queue driven executor for heavy FFmpeg slicing operations.
 */
@Slf4j
@Component
public class LocalSliceTaskExecutor {

    private final BlockingQueue<SliceTask> queue;
    private final ThreadPoolExecutor sliceExecutor;
    private final VideoTaskDomainService domainService;
    private final ITaskLogService taskLogService;

    public LocalSliceTaskExecutor(BlockingQueue<SliceTask> queue,
                                  ThreadPoolExecutor sliceExecutor,
                                  VideoTaskDomainService domainService,
                                  ITaskLogService taskLogService) {
        this.queue = queue;
        this.sliceExecutor = sliceExecutor;
        this.domainService = domainService;
        this.taskLogService = taskLogService;
    }

    /** Start worker threads that take tasks from local queue and process them. */
    @PostConstruct
    public void startWorkers() {
        int poolSize = sliceExecutor.getCorePoolSize();
        for (int i = 0; i < poolSize; i++) {
            sliceExecutor.execute(this::work);
        }
    }

    private void work() {
        while (true) {
            try {
                SliceTask task = queue.take();
                sliceOne(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void sliceOne(SliceTask sliceTask) {
        VideoUploadTask task = sliceTask.getUploadTask();
        try {
            domainService.processSlices(sliceTask.getProjectConfig(), task);
        } catch (Exception e) {
            log.error("slice task failed", e);
            taskLogService.error(task.getId(), "slice failed: " + e.getMessage());
        }
    }

    /**
     * Submit a new slicing task into local queue.
     *
     * @throws BusinessException when queue is full
     */
    public void submit(ProjectConfig config, VideoUploadTask task) {
        boolean offered = queue.offer(new SliceTask(config, task));
        if (!offered) {
            throw new BusinessException(ErrorCode.RESOURCE_EXHAUSTED,
                    "slice task queue full");
        }
    }
}
