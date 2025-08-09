package com.fab.video_convert_platform.domain.enums;

/**
 * Processing status for archived files.
 */
public enum ArchiveStatus {
    /**
     * File is active and available for use.
     */
    ACTIVE,
    
    /**
     * File has been archived (moved to long-term storage).
     */
    ARCHIVED,
    
    /**
     * File has been marked as deleted.
     */
    DELETED,
    
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
