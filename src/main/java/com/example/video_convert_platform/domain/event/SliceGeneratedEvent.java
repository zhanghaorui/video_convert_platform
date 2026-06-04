package com.example.video_convert_platform.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event fired when a video slice (m3u8) has been generated.
 */
@Getter
@AllArgsConstructor
public class SliceGeneratedEvent implements DomainEvent {
    private final Long taskId;
    private final String quality;
    private final String fileName;
    private final String filePath;
    private final String playUrl;
    private final long fileSize;
    private final String md5;
}
