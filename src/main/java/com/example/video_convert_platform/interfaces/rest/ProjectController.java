package com.example.video_convert_platform.interfaces.rest;

import com.example.video_convert_platform.common.ApiResponse;
import com.example.video_convert_platform.common.ErrorCode;
import com.example.video_convert_platform.interfaces.dto.request.ProjectConfigCreateRequest;
import com.example.video_convert_platform.domain.ProjectConfig;
import com.example.video_convert_platform.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Controller for project configuration CRUD operations.
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
@Validated
@Tag(name = "项目配置管理", description = "项目配置的增删改查接口")
public class ProjectController {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected service is managed externally")
    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目配置", description = "创建新的项目配置")
    public ApiResponse<ProjectConfig> create(@Valid @RequestBody ProjectConfigCreateRequest request) {
        // 转换DTO为领域对象，使用默认回调URL
        ProjectConfig config = ProjectConfig.create(
            request.getProjectNo(), 
            request.getProjectName(), 
            request.getArchiveRoot(),
            null // 默认无回调URL，后续可通过更新设置
        );
        
        return ApiResponse.success(projectService.create(config));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询项目配置", description = "根据ID查询项目配置")
    public ApiResponse<ProjectConfig> get(
            @Parameter(description = "项目配置ID") @PathVariable Long id) {
        return ApiResponse.success(projectService.getById(id));
    }

    @GetMapping
    @Operation(summary = "查询所有项目配置", description = "查询所有项目配置列表")
    public ApiResponse<List<ProjectConfig>> list() {
        return ApiResponse.success(projectService.list());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目配置", description = "根据ID更新项目配置")
    public ApiResponse<ProjectConfig> update(
            @Parameter(description = "项目配置ID") @PathVariable Long id,
                                             @RequestBody ProjectConfig config) {
        if (config.getId() != null && !config.getId().equals(id)) {
            return ApiResponse.failure(ErrorCode.PARAM_ERROR, "ID in path and body do not match");
        }
        config.setId(id);
        return ApiResponse.success(projectService.update(config));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目配置", description = "根据ID删除项目配置")
    public ApiResponse<Boolean> delete(
            @Parameter(description = "项目配置ID") @PathVariable Long id) {
        return ApiResponse.success(projectService.remove(id));
    }
}

