package com.fab.video_convert_platform.controller.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 视频上传请求DTO
 */
@Data
public class VideoUploadRequest {

    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    @NotBlank(message = "项目编号不能为空")
    private String projectNo;

    @NotBlank(message = "受试者编码不能为空")
    private String patientCode;

    @NotBlank(message = "访视点不能为空")
    private String tpStage;
}
