package com.fab.videoproject.service;

import com.fab.videoproject.domain.VideoUploadTask;

/**
 * Service responsible for notifying external systems when processing completes.
 */
public interface ICallbackService {

    /**
     * Notify business system that slices are ready for a task.
     *
     * @param task upload task
     */
    void notify(VideoUploadTask task);
}

