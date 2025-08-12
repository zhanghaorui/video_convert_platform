package com.fab.video_convert_platform.service.listener;

import com.fab.video_convert_platform.domain.event.SliceGeneratedEvent;
import com.fab.video_convert_platform.service.IArchiveService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Persists slice archive records when slices are generated.
 */
@Component
public class SliceGeneratedEventListener {

    private final IArchiveService archiveService;

    public SliceGeneratedEventListener(IArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @EventListener
    public void onSliceGenerated(SliceGeneratedEvent event) {
        archiveService.saveM3u8(event.getTaskId(), event.getQuality(), event.getFileName(),
            event.getFilePath(), event.getPlayUrl(), event.getFileSize(), event.getMd5());
    }
}
