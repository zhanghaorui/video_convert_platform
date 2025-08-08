package com.fab.videoproject.domain.enums;

/**
 * Processing status for archived files.
 */
public enum ArchiveStatus {
    /**
     * File has been stored but not yet fully processed.
     */
    SAVED,
    /**
     * File ready for consumption.
     */
    READY,
    /**
     * Processing failed.
     */
    FAILED
}
