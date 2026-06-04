package com.example.video_convert_platform.domain.infrastructure;

import java.nio.file.Path;
import java.util.List;

/**
 * 视频处理基础设施接口
 * 定义视频转码、切片等技术操作的抽象
 */
public interface VideoProcessingInfrastructure {

    /**
     * 检查视频文件完整性
     * @param videoPath 视频文件路径
     * @return 是否完整
     */
    boolean checkVideoIntegrity(Path videoPath);

    /**
     * 获取视频分辨率
     * @param videoPath 视频文件路径
     * @return 分辨率数组 [width, height]
     */
    int[] getVideoResolution(Path videoPath);

    /**
     * 检查视频格式是否为AVI
     * @param videoPath 视频文件路径
     * @return 是否为AVI格式
     */
    boolean isAviFormat(Path videoPath);

    /**
     * 将AVI格式转换为MP4
     * @param aviPath AVI文件路径
     * @param mp4Path 输出MP4文件路径
     */
    void convertAviToMp4(Path aviPath, Path mp4Path);

    /**
     * 降低视频分辨率到指定尺寸
     * @param inputPath 输入视频路径
     * @param outputPath 输出视频路径
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     */
    void downscaleVideo(Path inputPath, Path outputPath, int targetWidth, int targetHeight);

    /**
     * 将视频切片为HLS格式
     * @param inputPath 输入视频路径
     * @param outputDir 输出目录
     * @param segmentDuration 片段时长（秒）
     * @return m3u8文件路径
     */
    Path sliceToHls(Path inputPath, Path outputDir, int segmentDuration);

    /**
     * 批量清理临时文件
     * @param tempFiles 临时文件列表
     */
    void cleanupTempFiles(List<Path> tempFiles);
}
