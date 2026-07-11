package com.example.video_convert_platform.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.example.video_convert_platform.common.VideoConstants;
import com.example.video_convert_platform.common.BusinessException;
import com.example.video_convert_platform.common.ErrorCode;

/**
 * Utility for building standard archive paths on NFS.
 * 包含路径安全校验，防止路径遍历攻击。
 */
public final class ArchivePathUtil {

    private ArchivePathUtil() {
    }

    /**
     * 校验路径组件的安全性
     * 防止路径遍历攻击（如..）和特殊字符注入
     *
     * @param component 路径组件
     * @param componentName 组件名称（用于错误消息）
     * @throws BusinessException 如果路径组件不安全
     */
    public static void validatePathComponent(String component, String componentName) {
        if (component == null || component.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                componentName + "不能为空");
        }

        // 防止路径遍历攻击
        if (component.contains("..") || component.contains("./") || component.contains("/.")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                componentName + "包含非法路径序列: " + component);
        }

        // 防止绝对路径注入
        if (component.startsWith("/") || component.startsWith("\\")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                componentName + "不能以路径分隔符开头: " + component);
        }

        // 防止特殊字符注入（仅允许字母、数字、下划线、中划线）
        // 对于fileName，允许更宽松的字符集（包含点号）
        String safePattern = componentName.equals("fileName")
            ? "[a-zA-Z0-9_\\-.]+"
            : "[a-zA-Z0-9_\\-]+";
        if (!component.matches(safePattern)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                componentName + "包含非法字符: " + component);
        }
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
        // 校验所有路径组件
        validatePathComponent(projectNo, "projectNo");
        validatePathComponent(patientCode, "patientCode");
        validatePathComponent(tpStage, "tpStage");
        validatePathComponent(uuid, "uuid");
        validatePathComponent(fileName, "fileName");

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
        // 校验路径组件
        validatePathComponent(projectNo, "projectNo");
        validatePathComponent(patientCode, "patientCode");
        validatePathComponent(tpStage, "tpStage");
        validatePathComponent(uuid, "uuid");

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
        // 校验路径组件
        validatePathComponent(projectNo, "projectNo");
        validatePathComponent(patientCode, "patientCode");
        validatePathComponent(tpStage, "tpStage");
        validatePathComponent(uuid, "uuid");
        validatePathComponent(quality, "quality");

        String dir = VideoConstants.DIR_SLICE_PREFIX + quality;
        return Paths.get(root, projectNo, patientCode, tpStage,
                String.valueOf(versionNo), uuid, dir);
    }

    /**
     * Build relative play URL for HLS streaming
     * @param projectNo   project number
     * @param patientCode patient code  
     * @param tpStage     time point stage
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
