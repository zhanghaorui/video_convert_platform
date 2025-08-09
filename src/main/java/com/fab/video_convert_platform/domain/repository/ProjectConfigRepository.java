package com.fab.video_convert_platform.domain.repository;

import com.fab.video_convert_platform.domain.ProjectConfig;

import java.util.List;
import java.util.Optional;

/**
 * 项目配置仓储接口
 * 定义领域层对项目配置持久化的抽象
 */
public interface ProjectConfigRepository {

    /**
     * 保存项目配置
     * @param config 项目配置
     * @return 保存后的配置
     */
    ProjectConfig save(ProjectConfig config);

    /**
     * 根据ID查找项目配置
     * @param id 配置ID
     * @return 项目配置（可能为空）
     */
    Optional<ProjectConfig> findById(Long id);

    /**
     * 根据项目编号查找配置
     * @param projectNo 项目编号
     * @return 项目配置（可能为空）
     */
    Optional<ProjectConfig> findByProjectNo(String projectNo);

    /**
     * 查找所有活跃的项目配置
     * @return 活跃的项目配置列表
     */
    List<ProjectConfig> findAllActive();

    /**
     * 查找所有项目配置
     * @return 所有项目配置
     */
    List<ProjectConfig> findAll();

    /**
     * 删除项目配置
     * @param id 配置ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查项目编号是否已存在
     * @param projectNo 项目编号
     * @return 是否存在
     */
    boolean existsByProjectNo(String projectNo);
}
