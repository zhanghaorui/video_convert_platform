package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.fab.video_convert_platform.service.IProjectService;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of project service.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
public class ProjectServiceImpl implements IProjectService {

    @SuppressWarnings("EI_EXPOSE_REP2") // 依赖注入场景，预期行为
    private final ProjectConfigRepository projectConfigRepository;

    @Override
    public ProjectConfig create(ProjectConfig config) {
        return projectConfigRepository.save(config);
    }

    @Override
    public ProjectConfig getById(Long id) {
        return projectConfigRepository.findById(id).orElse(null);
    }

    @Override
    public List<ProjectConfig> list() {
        return projectConfigRepository.findAll();
    }

    @Override
    public ProjectConfig getByProjectNo(String projectNo) {
        return projectConfigRepository.findByProjectNo(projectNo).orElse(null);
    }

    @Override
    public ProjectConfig update(ProjectConfig config) {
        return projectConfigRepository.save(config);
    }

    @Override
    public boolean remove(Long id) {
        return projectConfigRepository.deleteById(id);
    }
}
