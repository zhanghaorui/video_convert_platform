package com.fab.video_convert_platform.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 文件信息值对象
 * 封装文件大小、MD5等信息及其业务规则
 */
@Getter
@ToString
@EqualsAndHashCode
public class FileInfo {
    
    private final String fileName;
    private final Long fileSize;
    private final String fileMd5;

    /**
     * 私有构造函数
     */
    private FileInfo(String fileName, Long fileSize, String fileMd5) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileMd5 = fileMd5;
    }

    /**
     * 创建文件信息值对象
     * @param fileName 文件名
     * @param fileSize 文件大小（字节）
     * @param fileMd5 文件MD5值
     * @return 文件信息值对象
     */
    public static FileInfo of(String fileName, Long fileSize, String fileMd5) {
        validateFileInfo(fileName, fileSize, fileMd5);
        return new FileInfo(fileName, fileSize, fileMd5);
    }

    /**
     * 验证文件信息
     */
    private static void validateFileInfo(String fileName, Long fileSize, String fileMd5) {
        if (Objects.isNull(fileName) || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        if (Objects.isNull(fileSize) || fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必须大于0");
        }
        
        if (Objects.isNull(fileMd5) || fileMd5.trim().isEmpty()) {
            throw new IllegalArgumentException("文件MD5不能为空");
        }
        
        // MD5应该是32位十六进制字符串
        String trimmedMd5 = fileMd5.trim();
        if (trimmedMd5.length() != 32 || !trimmedMd5.matches("^[a-fA-F0-9]+$")) {
            throw new IllegalArgumentException("MD5格式不正确，应为32位十六进制字符串");
        }
    }

    /**
     * 获取文件扩展名
     * @return 文件扩展名（不包含点号）
     */
    public String getFileExtension() {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 判断是否为视频文件
     * @return 是否为视频文件
     */
    public boolean isVideoFile() {
        String extension = getFileExtension();
        return "mp4".equals(extension) || "avi".equals(extension) || 
               "mov".equals(extension) || "wmv".equals(extension) ||
               "flv".equals(extension) || "mkv".equals(extension);
    }

    /**
     * 获取可读的文件大小
     * @return 可读的文件大小字符串
     */
    public String getReadableFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 判断是否为大文件
     * 业务规则：超过100MB的文件认为是大文件
     * @return 是否为大文件
     */
    public boolean isLargeFile() {
        return fileSize > 100 * 1024 * 1024; // 100MB
    }
}
