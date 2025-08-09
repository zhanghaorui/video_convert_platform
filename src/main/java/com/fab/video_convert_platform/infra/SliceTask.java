package com.fab.video_convert_platform.infra;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for slicing task data pulled from local queue.
 */
@AllArgsConstructor
@Getter
public class SliceTask {
    private final ProjectConfig projectConfig;
    private final VideoUploadTask uploadTask;
}
