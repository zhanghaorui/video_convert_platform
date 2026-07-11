package com.example.video_convert_platform.interfaces.rest;

import com.example.video_convert_platform.common.ApiResponse;
import com.example.video_convert_platform.common.ErrorCode;
import com.example.video_convert_platform.common.BusinessException; // add
import com.example.video_convert_platform.interfaces.dto.request.*;
import com.example.video_convert_platform.interfaces.dto.response.*;
import com.example.video_convert_platform.domain.VideoUploadTask;
import com.example.video_convert_platform.domain.VideoArchiveFile;
import com.example.video_convert_platform.service.VideoService;
import com.example.video_convert_platform.config.NfsProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Validated
@Tag(name = "视频处理", description = "视频上传、转码、切片和播放URL查询接口")
public class VideoController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed externally")
    private final VideoService videoService;
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
    @Operation(summary = "上传完整视频", description = "上传完整视频文件并进行归档和转码处理")
    public ApiResponse<VideoUploadResponse> uploadVideo(
            @Parameter(description = "视频文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "项目编号") @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @Parameter(description = "受试者编码") @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @Parameter(description = "访视点") @RequestParam @NotBlank(message = "访视点不能为空") String tpStage) {

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
    @Operation(summary = "分片上传视频", description = "分片上传视频文件，最后一片时自动合并并处理")
    public ApiResponse<String> uploadVideoChunk(
            @Parameter(description = "分片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "项目编号") @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @Parameter(description = "受试者编码") @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @Parameter(description = "访视点") @RequestParam @NotBlank(message = "访视点不能为空") String tpStage,
            @Parameter(description = "原始文件名") @RequestParam @NotBlank(message = "文件名不能为空") String filename,
            @Parameter(description = "上传会话UUID") @RequestParam @NotBlank(message = "UUID不能为空") String uuid,
            @Parameter(description = "当前分片索引") @RequestParam(required = false) Integer chunk,
            @Parameter(description = "总分片数") @RequestParam(required = false) Integer chunks,
            @Parameter(description = "访视信息") @RequestParam(required = false) String visit) { // 新增 visit 可选参数

        log.info("开始上传视频分片: projectNo={}, patientCode={}, tpStage={}, filename={}, chunk={}/{}, visit={}",
                projectNo, patientCode, tpStage, filename, chunk, chunks, visit);

        videoService.uploadChunk(file, chunk, chunks, filename, projectNo, patientCode, tpStage, uuid, visit);

        String message = (chunk != null && chunks != null && chunk + 1 == chunks)
                ? "分片上传完成，开始合并处理"
                : "分片上传成功";

        log.info("视频分片上传成功: chunk={}/{}, visit={}", chunk, chunks, visit);
        return ApiResponse.success(message);
    }

    /**
     * 查询上传任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态", description = "根据任务ID查询上传任务的处理状态")
    public ApiResponse<VideoUploadResponse> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
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
        response.setVisit(task.getVisit()); // 回填 visit
        response.setCheckDate(task.getCheckDate()); // 回填 checkDate
        return response;
    }

    /**
     * 根据任务ID查询播放URL
     *
     * @param taskId 任务ID
     * @return 播放URL信息
     */
    @GetMapping("/tasks/{taskId}/play-urls")
    @Operation(summary = "查询播放URL（按任务ID）", description = "根据任务ID查询该任务生成的HLS播放地址")
    public ApiResponse<List<PlayUrlInfo>> getPlayUrlsByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
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
    @Operation(summary = "查询播放URL（按业务参数）", description = "根据项目号、受试者编码等业务参数查询HLS播放地址")
    public ApiResponse<PlayUrlResponse> getPlayUrlsByParams(
            @Parameter(description = "项目编号") @RequestParam @NotBlank(message = "项目编号不能为空") String projectNo,
            @Parameter(description = "受试者编码") @RequestParam @NotBlank(message = "受试者编码不能为空") String patientCode,
            @Parameter(description = "访视点（与visit二选一）") @RequestParam(required = false) String tpStage,
            @Parameter(description = "访视信息（与tpStage二选一）") @RequestParam(required = false) String visit,
            @Parameter(description = "版本号") @RequestParam(required = false) Integer versionNo,
            @Parameter(description = "质量级别") @RequestParam(required = false) String quality) {

        log.info("查询播放URL: projectNo={}, patientCode={}, tpStage={}, visit={}, versionNo={}, quality={}",
                projectNo, patientCode, tpStage, visit, versionNo, quality);

        boolean hasTp = tpStage != null && !tpStage.trim().isEmpty();
        boolean hasVisit = visit != null && !visit.trim().isEmpty();
        if (hasTp == hasVisit) { // 同时传或都不传
            throw new BusinessException(ErrorCode.PARAM_ERROR, "tpStage与visit必须且只能传一个");
        }

        List<VideoArchiveFile> archiveFiles = videoService.getPlayUrlsByParams(
                projectNo, patientCode, tpStage, visit, versionNo, quality);

        // 按任务分组构建响应
        Map<Long, List<VideoArchiveFile>> taskGroups = archiveFiles.stream()
                .collect(Collectors.groupingBy(VideoArchiveFile::getTaskId));

        List<PlayUrlTaskInfo> tasks = taskGroups.entrySet().stream()
                .map(entry -> {
                    Long taskId = entry.getKey();
                    List<VideoArchiveFile> files = entry.getValue();
                    VideoUploadTask task = videoService.getTaskById(taskId);
                    PlayUrlTaskInfo taskInfo = new PlayUrlTaskInfo();
                    taskInfo.setTaskId(taskId);
                    taskInfo.setUuid(task.getUuid());
                    taskInfo.setVersionNo(task.getVersionNo());
                    taskInfo.setStatus(task.getStatus());
                    taskInfo.setCheckDate(task.getCheckDate()); // 设置检查日期
                    taskInfo.setCreateTime(task.getCreateTime());
                    List<PlayUrlInfo> playUrls = files.stream()
                            .map(this::convertToPlayUrlInfo)
                            .collect(Collectors.toList());
                    taskInfo.setPlayUrls(playUrls);
                    return taskInfo;
                })
                .sorted((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime()))
                .collect(Collectors.toList());

        PlayUrlResponse response = new PlayUrlResponse();
        response.setProjectNo(projectNo);
        response.setPatientCode(patientCode);
        if (hasTp) {
            response.setTpStage(tpStage);
        } else {
            response.setVisit(visit);
        }
        response.setTasks(tasks);
        response.setTotalTasks(tasks.size());
        response.setTotalPlayUrls(archiveFiles.size());

        log.info("查询到{}个任务，{}个播放URL (by {})", tasks.size(), archiveFiles.size(), hasTp ? "tpStage" : "visit");
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
     * 支持三种URL模式：
     * 1. 完整URL（以http/https开头）- 直接返回
     * 2. 相对路径 - 拼接baseUrl
     * 3. 绝对路径（以/开头）- 拼接baseUrl
     */
    private String buildFullPlayUrl(String storedUrl) {
        if (!StringUtils.hasText(storedUrl)) {
            return null;
        }

        // 如果已经是完整URL，直接返回
        if (storedUrl.startsWith("http://") || storedUrl.startsWith("https://")) {
            return storedUrl;
        }

        String baseUrl = nfsProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return storedUrl; // 如果没有配置baseUrl，返回原路径
        }
        
        // 确保baseUrl不以/结尾，storedUrl以/开头
        String cleanBaseUrl = baseUrl.replaceAll("/$", "");
        String cleanRelativePath = storedUrl.startsWith("/") ? storedUrl : "/" + storedUrl;
        
        // 直接拼接URL，不进行去重处理
        // 因为项目编号在路径中重复出现是正常的业务逻辑
        String fullUrl = cleanBaseUrl + cleanRelativePath;
        
        return fullUrl;
    }
}
