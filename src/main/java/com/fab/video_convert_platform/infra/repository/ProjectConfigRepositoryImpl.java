package com.fab.video_convert_platform.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目配置仓储实现
 * 基础设施层，负责具体的数据持久化操作
 */
@Repository
public class ProjectConfigRepositoryImpl implements ProjectConfigRepository {

    private final ProjectConfigMapper mapper;

    public ProjectConfigRepositoryImpl(ProjectConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ProjectConfig save(ProjectConfig config) {
        if (config.getId() == null) {
            mapper.insert(config);
        } else {
            mapper.updateById(config);
        }
        return mapper.selectById(config.getId());
    }

    @Override
    public Optional<ProjectConfig> findById(Long id) {
        ProjectConfig config = mapper.selectById(id);
        return Optional.ofNullable(config);
    }

    @Override
    public Optional<ProjectConfig> findByProjectNo(String projectNo) {
        ProjectConfig config = mapper.selectOne(
            new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getProjectNo, projectNo)
                .last("LIMIT 1")
        );
        return Optional.ofNullable(config);
    }

    @Override
    public List<ProjectConfig> findAllActive() {
        return mapper.selectList(
            new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getIsActive, true)
                .orderByDesc(ProjectConfig::getCreateTime)
        );
    }

    @Override
    public List<ProjectConfig> findAll() {
        return mapper.selectList(
            new LambdaQueryWrapper<ProjectConfig>()
                .orderByDesc(ProjectConfig::getCreateTime)
        );
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public boolean existsByProjectNo(String projectNo) {
        Long count = mapper.selectCount(
            new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getProjectNo, projectNo)
        );
        return count > 0;
    }
}
