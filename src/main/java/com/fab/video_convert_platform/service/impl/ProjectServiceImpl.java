package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import com.fab.video_convert_platform.service.IProjectService;
import org.springframework.stereotype.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of project service.
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private final ProjectConfigMapper projectConfigMapper;

    @Override
    public ProjectConfig create(ProjectConfig config) {
        projectConfigMapper.insert(config);
        return projectConfigMapper.selectById(config.getId());
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
    public ProjectConfig getByProjectNo(String projectNo) {
        return projectConfigMapper.selectOne(new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getProjectNo, projectNo)
                .last("limit 1"));
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
