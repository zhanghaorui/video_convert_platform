package com.fab.video_convert_platform.domain.enums;

/**
 * Main status for video upload tasks.
 */
public enum TaskStatus {
    /**
     * Original file has been saved to NFS and task is waiting for further processing.
     */
    ORIGINAL_SAVED,

    /**
     * Task is currently being processed (video conversion, slicing, etc.).
     */
    PROCESSING,

    /**
     * All processing completed successfully.
     */
    FINISHED,

    /**
     * Task failed.
     */
    FAILED
}
