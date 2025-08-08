package com.fab.video_convert_platform.controller;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.IVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REST controller for managing video operations.
 */
@RestController
@RequestMapping("/videos")
public class VideoController {

    @Autowired
    private IVideoService videoService;

    /**
     * Upload complete video and archive original file.
     */
    @PostMapping("/upload")
    public ApiResponse<VideoUploadTask> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam String projectNo,
                                               @RequestParam String patientCode,
                                               @RequestParam String tpStage) {
        return ApiResponse.success(
                videoService.upload(file, projectNo, patientCode, tpStage));
    }

    /**
     * Chunked upload of video files.
     */
    @PostMapping("/upload/chunk")
    public void upload(HttpServletRequest request,
                       HttpServletResponse response,
                       @RequestParam("file") MultipartFile file,
                       @RequestParam(value = "chunk", required = false) Integer chunk,
                       @RequestParam(value = "chunks", required = false) Integer chunks,
                       @RequestParam("filename") String filename,
                       @RequestParam("patientCode") String patientCode,
                       @RequestParam("tpStage") String tpStage,
                       @RequestParam("uuid") String uuid,
                       @RequestParam("projectNo") String projectNo) {
        videoService.uploadChunk(file, chunk, chunks, filename, projectNo, patientCode, tpStage, uuid);
    }
}
