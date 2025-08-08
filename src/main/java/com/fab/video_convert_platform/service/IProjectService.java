package com.fab.video_convert_platform.service;

import com.fab.video_convert_platform.domain.ProjectConfig;
import java.util.List;

/**
 * Interface for project-related operations.
 */
public interface IProjectService {
    /**
     * Create a new project configuration.
     * @param config project configuration
     * @return saved configuration
     */
    ProjectConfig create(ProjectConfig config);

    /**
     * Retrieve a configuration by id.
     * @param id primary key
     * @return configuration or null
     */
    ProjectConfig getById(Long id);

    /**
     * List all project configurations.
     * @return list of configurations
     */
    List<ProjectConfig> list();

    /**
     * Retrieve a configuration by project number.
     *
     * @param projectNo unique project identifier
     * @return configuration or {@code null} if not found
     */
    ProjectConfig getByProjectNo(String projectNo);

    /**
     * Update an existing configuration.
     * @param config configuration with id
     * @return updated configuration
     */
    ProjectConfig update(ProjectConfig config);

    /**
     * Remove configuration by id.
     * @param id primary key
     * @return true if removed
     */
    boolean remove(Long id);
}
