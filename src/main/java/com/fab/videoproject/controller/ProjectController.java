package com.fab.videoproject.controller;

import com.fab.videoproject.common.ApiResponse;
import com.fab.videoproject.domain.ProjectConfig;
import com.fab.videoproject.service.IProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for project configuration CRUD operations.
 */
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private IProjectService projectService;

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
        config.setId(id);
        return ApiResponse.success(projectService.update(config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(projectService.remove(id));
    }
}

