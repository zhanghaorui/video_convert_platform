package com.fab.videoproject.service.impl;

import com.fab.videoproject.service.IProjectService;
import org.springframework.stereotype.Service;

/**
 * Implementation of project service.
 */
@Service
public class ProjectServiceImpl implements IProjectService {
    @Override
    public String ping() {
        return "project service pong";
    }
}
