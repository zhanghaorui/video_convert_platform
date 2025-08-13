package com.fab.video_convert_platform.interfaces.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 项目配置创建请求DTO
 *
 * @author zhanghaorui
 */
@Data
public class ProjectConfigCreateRequest {

    @NotBlank(message = "项目编号不能为空")
    @Size(max = 50, message = "项目编号长度不能超过50位")
    private String projectNo;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100位")
    private String projectName;

    @NotBlank(message = "归档根目录不能为空")
    @Size(max = 200, message = "归档根目录长度不能超过200位")
    private String archiveRoot;

    @Size(max = 200, message = "项目描述长度不能超过200位")
    private String description;

    private Boolean isActive = true;
}
