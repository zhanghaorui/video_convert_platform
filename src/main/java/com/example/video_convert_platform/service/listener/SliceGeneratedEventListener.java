package com.example.video_convert_platform.service.listener;

import com.example.video_convert_platform.domain.event.SliceGeneratedEvent;
import com.example.video_convert_platform.service.ArchiveService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists slice archive records when slices are generated.
 */
@Component
public class SliceGeneratedEventListener {

    private final ArchiveService archiveService;

    public SliceGeneratedEventListener(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @EventListener
    @Transactional
    public void onSliceGenerated(SliceGeneratedEvent event) {
        archiveService.saveM3u8(event.getTaskId(), event.getQuality(), event.getFileName(),
            event.getFilePath(), event.getPlayUrl(), event.getFileSize(), event.getMd5());
    }
}
