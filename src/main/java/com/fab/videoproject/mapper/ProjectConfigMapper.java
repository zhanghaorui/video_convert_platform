package com.fab.videoproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.videoproject.domain.ProjectConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for project configuration.
 */
@Mapper
public interface ProjectConfigMapper extends BaseMapper<ProjectConfig> {
}

