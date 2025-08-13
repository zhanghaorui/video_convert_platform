package com.fab.video_convert_platform.interfaces.rest;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.interfaces.dto.request.*;
import com.fab.video_convert_platform.interfaces.dto.response.*;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.service.IVideoService;
import com.fab.video_convert_platform.config.NfsProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视频处理控制器
 * 遵循RESTful设计规范和P3C编码规约
 * @author 张浩锐
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Validated
public class VideoController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed externally")
    private final IVideoService videoService;
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration properties are immutable")
    private final NfsProperties nfsProperties;

    /**
     * 上传完整视频文件
     *
     * @param file       上传的视频文件
     * @param projectNo  项目编号
     * @param patientCode 受试者编码
     * @param tpStage    访视点
     * @return 上传任务信息
     */
    @PostMapping("/upload")
    public ApiResponse<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @RequestParam @NotBlank(message = "访视点不能为空") String tpStage) {

        log.info("开始上传视频文件: projectNo={}, patientCode={}, tpStage={}, fileName={}",
                projectNo, patientCode, tpStage, file.getOriginalFilename());

        VideoUploadTask task = videoService.upload(file, projectNo, patientCode, tpStage);
        VideoUploadResponse response = convertToResponse(task);

        log.info("视频文件上传成功: taskId={}", task.getId());
        return ApiResponse.success(response);
    }

    /**
     * 分片上传视频文件
     *
     * @param file       分片文件
     * @param projectNo  项目编号
     * @param patientCode 受试者编码
     * @param tpStage    访视点
     * @param filename   原始文件名
     * @param uuid       上传会话唯一标识
     * @param chunk      当前分片索引
     * @param chunks     总分片数
     * @return 上传结果
     */
    @PostMapping("/upload/chunk")
    public ApiResponse<String> uploadVideoChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @RequestParam @NotBlank(message = "访视点不能为空") String tpStage,
            @RequestParam @NotBlank(message = "文件名不能为空") String filename,
            @RequestParam @NotBlank(message = "UUID不能为空") String uuid,
            @RequestParam(required = false) Integer chunk,
            @RequestParam(required = false) Integer chunks) {

        log.info("开始上传视频分片: projectNo={}, patientCode={}, tpStage={}, filename={}, chunk={}/{}",
                projectNo, patientCode, tpStage, filename, chunk, chunks);

        videoService.uploadChunk(file, chunk, chunks, filename, projectNo, patientCode, tpStage, uuid);

        String message = (chunk != null && chunks != null && chunk + 1 == chunks)
                ? "分片上传完成，开始合并处理"
                : "分片上传成功";

        log.info("视频分片上传成功: chunk={}/{}", chunk, chunks);
        return ApiResponse.success(message);
    }

    /**
     * 查询上传任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<VideoUploadResponse> getTaskStatus(@PathVariable Long taskId) {
        log.info("查询上传任务状态: taskId={}", taskId);

        VideoUploadTask task = videoService.getTaskById(taskId);
        VideoUploadResponse response = convertToResponse(task);

        return ApiResponse.success(response);
    }

    /**
     * 将领域实体转换为响应DTO
     * 遵循DDD规约，不直接暴露领域实体
     */
    private VideoUploadResponse convertToResponse(VideoUploadTask task) {
        VideoUploadResponse response = new VideoUploadResponse();
        response.setTaskId(task.getId());
        response.setProjectNo(task.getProjectNo());
        response.setPatientCode(task.getPatientCode());
        response.setTpStage(task.getTpStage());
        response.setUuid(task.getUuid());
        response.setVersionNo(task.getVersionNo());
        response.setStatus(task.getStatus());
        response.setFileName(task.getMainFileName());
        response.setFileSize(task.getFileSize());
        response.setCreateTime(task.getCreateTime());
        response.setErrorMsg(task.getErrorMsg());
        return response;
    }

    /**
     * 根据任务ID查询播放URL
     *
     * @param taskId 任务ID
     * @return 播放URL信息
     */
    @GetMapping("/tasks/{taskId}/play-urls")
    public ApiResponse<List<PlayUrlInfo>> getPlayUrlsByTaskId(@PathVariable Long taskId) {
        log.info("查询任务播放URL: taskId={}", taskId);

        List<VideoArchiveFile> archiveFiles = videoService.getPlayUrlsByTaskId(taskId);
        List<PlayUrlInfo> playUrls = archiveFiles.stream()
                .map(this::convertToPlayUrlInfo)
                .collect(Collectors.toList());

        log.info("查询到{}个播放URL: taskId={}", playUrls.size(), taskId);
        return ApiResponse.success(playUrls);
    }

    /**
     * 根据业务参数查询播放URL
     *
     * @param projectNo   项目编号
     * @param patientCode 受试者编码
     * @param tpStage     访视点
     * @param versionNo   版本号（可选）
     * @param quality     质量级别（可选）
     * @return 播放URL响应
     */
    @GetMapping("/play-urls")
    public ApiResponse<PlayUrlResponse> getPlayUrlsByParams(
            @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @RequestParam @NotBlank(message = "访视点不能为空") String tpStage,
            @RequestParam(required = false) Integer versionNo,
            @RequestParam(required = false) String quality) {

        log.info("查询播放URL: projectNo={}, patientCode={}, tpStage={}, versionNo={}, quality={}",
                projectNo, patientCode, tpStage, versionNo, quality);

        List<VideoArchiveFile> archiveFiles = videoService.getPlayUrlsByParams(
                projectNo, patientCode, tpStage, versionNo, quality);

        // 按任务分组构建响应
        Map<Long, List<VideoArchiveFile>> taskGroups = archiveFiles.stream()
                .collect(Collectors.groupingBy(VideoArchiveFile::getTaskId));

        List<PlayUrlTaskInfo> tasks = taskGroups.entrySet().stream()
                .map(entry -> {
                    Long taskId = entry.getKey();
                    List<VideoArchiveFile> files = entry.getValue();
                    
                    // 获取任务信息（使用第一个文件关联的任务ID）
                    VideoUploadTask task = videoService.getTaskById(taskId);
                    
                    PlayUrlTaskInfo taskInfo = new PlayUrlTaskInfo();
                    taskInfo.setTaskId(taskId);
                    taskInfo.setUuid(task.getUuid());
                    taskInfo.setVersionNo(task.getVersionNo());
                    taskInfo.setStatus(task.getStatus());
                    taskInfo.setCreateTime(task.getCreateTime());
                    
                    List<PlayUrlInfo> playUrls = files.stream()
                            .map(this::convertToPlayUrlInfo)
                            .collect(Collectors.toList());
                    taskInfo.setPlayUrls(playUrls);
                    
                    return taskInfo;
                })
                .sorted((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime())) // 按创建时间倒序
                .collect(Collectors.toList());

        PlayUrlResponse response = new PlayUrlResponse();
        response.setProjectNo(projectNo);
        response.setPatientCode(patientCode);
        response.setTpStage(tpStage);
        response.setTasks(tasks);
        response.setTotalTasks(tasks.size());
        response.setTotalPlayUrls(archiveFiles.size());

        log.info("查询到{}个任务，{}个播放URL", tasks.size(), archiveFiles.size());
        return ApiResponse.success(response);
    }

    /**
     * 将VideoArchiveFile转换为PlayUrlInfo
     */
    private PlayUrlInfo convertToPlayUrlInfo(VideoArchiveFile file) {
        PlayUrlInfo info = new PlayUrlInfo();
        info.setQuality(file.getQualityLevel());
        
        // 构建完整的播放URL
        String fullPlayUrl = buildFullPlayUrl(file.getPlayUrl());
        info.setPlayUrl(fullPlayUrl);
        
        info.setFileType(file.getFileType());
        info.setFileName(file.getFileName());
        info.setFileSize(file.getFileSize());
        info.setStatus(file.getStatus());
        info.setCreateTime(file.getCreateTime());
        return info;
    }

    /**
     * 构建完整的播放URL
     */
    private String buildFullPlayUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }

        String baseUrl = nfsProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return relativePath; // 如果没有配置baseUrl，返回相对路径
        }
        
        // 确保baseUrl不以/结尾，relativePath以/开头
        String cleanBaseUrl = baseUrl.replaceAll("/$", "");
        String cleanRelativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        
        return cleanBaseUrl + cleanRelativePath;
    }
}
