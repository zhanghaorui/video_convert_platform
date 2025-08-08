package com.fab.video_convert_platform.controller;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for project configuration CRUD operations.
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    @PostMapping
    public ApiResponse<ProjectConfig> create(@RequestBody ProjectConfig config) {
        return ApiResponse.success(projectService.create(config));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectConfig> get(@PathVariable Long id) {
        return ApiResponse.success(projectService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<ProjectConfig>> list() {
        return ApiResponse.success(projectService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectConfig> update(@PathVariable Long id,
                                             @RequestBody ProjectConfig config) {
        if (config.getId() != null && !config.getId().equals(id)) {
            return ApiResponse.failure(ErrorCode.PARAM_ERROR, "ID in path and body do not match");
        }
        config.setId(id);
        return ApiResponse.success(projectService.update(config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(projectService.remove(id));
    }
}

