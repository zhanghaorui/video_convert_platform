package com.fab.videoproject.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fab.videoproject.common.VideoConstants;

/**
 * Utility for building standard archive paths on NFS.
 */
public final class ArchivePathUtil {

    private ArchivePathUtil() {
    }

    /**
     * Build path for original video file.
     *
     * @param root       archive root directory
     * @param projectNo  project number
     * @param patientCode subject identifier
     * @param tpStage    visit stage
     * @param versionNo  version number
     * @param uuid       unique id
     * @param fileName   original file name
     * @return resolved path for storage
     */
    public static Path buildOriginalPath(String root,
                                         String projectNo,
                                         String patientCode,
                                         String tpStage,
                                         int versionNo,
                                         String uuid,
                                         String fileName) {
        return Paths.get(root, projectNo, patientCode, tpStage,
                String.valueOf(versionNo), uuid, VideoConstants.DIR_ORIGINAL, fileName);
    }

    /**
     * Build directory path for storing chunks before merge.
     */
    public static Path buildChunkPath(String root,
                                      String projectNo,
                                      String patientCode,
                                      String tpStage,
                                      int versionNo,
                                      String uuid) {
        return Paths.get(root, projectNo, patientCode, tpStage,
                String.valueOf(versionNo), uuid, VideoConstants.DIR_CHUNK);
    }
}
