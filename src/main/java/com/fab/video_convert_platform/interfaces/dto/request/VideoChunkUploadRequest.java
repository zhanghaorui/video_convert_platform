package com.fab.video_convert_platform.interfaces.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 视频分片上传请求DTO
 */
@Data
public class VideoChunkUploadRequest {

    @NotNull(message = "分片文件不能为空")
    private MultipartFile file;

    @NotBlank(message = "项目编号不能为空")
    private String projectNo;

    @NotBlank(message = "受试者编码不能为空")
    private String patientCode;

    @NotBlank(message = "访视点不能为空")
    private String tpStage;

    @NotBlank(message = "文件名不能为空")
    private String filename;

    @NotBlank(message = "UUID不能为空")
    private String uuid;

    @Min(value = 0, message = "分片索引不能小于0")
    private Integer chunk;

    @Min(value = 1, message = "总分片数不能小于1")
    private Integer chunks;
}
