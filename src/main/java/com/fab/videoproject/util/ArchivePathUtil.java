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

    /**
     * Build directory path for slices of specified quality.
     */
    public static Path buildSlicePath(String root,
                                      String projectNo,
                                      String patientCode,
                                      String tpStage,
                                      int versionNo,
                                      String uuid,
                                      String quality) {
        String dir = VideoConstants.DIR_SLICE_PREFIX + quality;
        return Paths.get(root, projectNo, patientCode, tpStage,
                String.valueOf(versionNo), uuid, dir);
    }

    /**
     * Build relative play URL for a generated m3u8 file.
     * The URL always uses forward slashes regardless of OS.
     *
     * @param projectNo   project number
     * @param patientCode subject identifier
     * @param tpStage     visit stage
     * @param versionNo   version number
     * @param uuid        unique id
     * @param quality     quality level
     * @return relative play URL such as {@code project/patient/tp/1/uuid/slice_low/index.m3u8}
     */
    public static String buildPlayUrl(String projectNo,
                                      String patientCode,
                                      String tpStage,
                                      int versionNo,
                                      String uuid,
                                      String quality) {
        String dir = VideoConstants.DIR_SLICE_PREFIX + quality;
        return String.join("/", projectNo, patientCode, tpStage,
                String.valueOf(versionNo), uuid, dir, VideoConstants.M3U8_NAME);
    }
}
