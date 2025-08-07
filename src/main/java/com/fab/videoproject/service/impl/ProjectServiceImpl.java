package com.fab.videoproject.service.impl;

import com.fab.videoproject.domain.ProjectConfig;
import com.fab.videoproject.mapper.ProjectConfigMapper;
import com.fab.videoproject.service.IProjectService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * Implementation of project service.
 */
@Service
public class ProjectServiceImpl implements IProjectService {

    @Autowired
    private ProjectConfigMapper projectConfigMapper;

    @Override
    public ProjectConfig create(ProjectConfig config) {
        projectConfigMapper.insert(config);
        return config;
    }

    @Override
    public ProjectConfig getById(Long id) {
        return projectConfigMapper.selectById(id);
    }

    @Override
    public List<ProjectConfig> list() {
        return projectConfigMapper.selectList(null);
    }

    @Override
    public ProjectConfig update(ProjectConfig config) {
        projectConfigMapper.updateById(config);
        return projectConfigMapper.selectById(config.getId());
    }

    @Override
    public boolean remove(Long id) {
        return projectConfigMapper.deleteById(id) > 0;
    }
}
