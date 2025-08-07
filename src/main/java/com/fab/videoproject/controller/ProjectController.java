package com.fab.videoproject.controller;

import com.fab.videoproject.service.IProjectService;
import com.fab.videoproject.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller for project operations.
 */
@RestController
public class ProjectController {

    @Resource
    private IProjectService projectService;

    @GetMapping("/project/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(projectService.ping());
    }
}

