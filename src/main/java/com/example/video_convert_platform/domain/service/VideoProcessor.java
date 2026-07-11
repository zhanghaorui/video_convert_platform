package com.example.video_convert_platform.domain.service;

import com.example.video_convert_platform.util.FFmpegUtil.VideoStreamInfo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 视频处理器接口
 * 定义视频处理的核心能力，领域层通过此接口与基础设施解耦
 */
public interface VideoProcessor {

    /**
     * 验证视频文件完整性
     *
     * @param input 输入视频文件
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    void validate(Path input) throws IOException, InterruptedException;

    /**
     * 快速验证视频文件（仅检查前几秒）
     *
     * @param input 输入视频文件
     * @param durationSeconds 检查的时长（秒）
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    void validateQuick(Path input, int durationSeconds) throws IOException, InterruptedException;

    /**
     * AVI格式转MP4
     *
     * @param input 输入文件
     * @param output 输出文件
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    void aviToMp4(Path input, Path output) throws IOException, InterruptedException;

    /**
     * 视频转码（指定分辨率）
     *
     * @param input 输入文件
     * @param output 输出文件
     * @param width 目标宽度
     * @param height 目标高度
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    void transcode(Path input, Path output, int width, int height) throws IOException, InterruptedException;

    /**
     * 视频转码（指定分辨率和码率）
     *
     * @param input 输入文件
     * @param output 输出文件
     * @param width 目标宽度
     * @param height 目标高度
     * @param bitrateKbps 目标码率（kbps），null表示使用默认
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    void transcode(Path input, Path output, int width, int height, Integer bitrateKbps)
            throws IOException, InterruptedException;

    /**
     * 视频切片为HLS格式
     *
     * @param input 输入视频文件
     * @param outputDir 输出目录
     * @return 生成的m3u8文件路径
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    Path sliceToM3u8(Path input, Path outputDir) throws IOException, InterruptedException;

    /**
     * 探测视频流信息
     *
     * @param input 输入视频文件
     * @return 视频流信息
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    VideoStreamInfo probeVideoStreamInfo(Path input) throws IOException, InterruptedException;

    /**
     * 获取视频分辨率
     *
     * @param input 输入视频文件
     * @return [width, height]
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    int[] getResolution(Path input) throws IOException, InterruptedException;

    /**
     * 获取视频码率
     *
     * @param input 输入视频文件
     * @return 码率（kbps），失败返回null
     * @throws IOException 文件操作异常
     * @throws InterruptedException 处理被中断
     */
    Integer getVideoBitrateKbps(Path input) throws IOException, InterruptedException;

    /**
     * 获取视频时长
     *
     * @param input 输入视频文件
     * @return 时长（秒）
     */
    double getVideoDurationSeconds(Path input);
}