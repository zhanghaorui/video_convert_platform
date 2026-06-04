package com.example.video_convert_platform.service;

import com.example.video_convert_platform.domain.VideoUploadTaskView;

/**
 * Service responsible for notifying external systems when processing completes.
 */
public interface ICallbackService {

    /**
     * Notify business system that slices are ready for a task.
     *
     * @param taskView upload task view
     */
    void notify(VideoUploadTaskView taskView);
}
