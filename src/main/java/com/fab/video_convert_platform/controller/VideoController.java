package com.fab.video_convert_platform.controller;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.controller.dto.VideoChunkUploadRequest;
import com.fab.video_convert_platform.controller.dto.VideoUploadRequest;
import com.fab.video_convert_platform.controller.dto.VideoUploadResponse;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.IVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 视频处理控制器
 * 遵循RESTful设计规范和P3C编码规约
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Validated
public class VideoController {

    private final IVideoService videoService;

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
}
