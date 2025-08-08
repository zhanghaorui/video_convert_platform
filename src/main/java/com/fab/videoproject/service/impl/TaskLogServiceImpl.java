package com.fab.videoproject.service.impl;

import com.fab.videoproject.domain.VideoTaskError;
import com.fab.videoproject.domain.VideoTaskInfo;
import com.fab.videoproject.mapper.VideoTaskErrorMapper;
import com.fab.videoproject.mapper.VideoTaskInfoMapper;
import com.fab.videoproject.service.ITaskLogService;
import com.fab.videoproject.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of task logging service.
 */
@Service
public class TaskLogServiceImpl implements ITaskLogService {

    @Autowired
    private VideoTaskInfoMapper infoMapper;

    @Autowired
    private VideoTaskErrorMapper errorMapper;

    @Override
    public void info(Long taskId, String message) {
        if (taskId == null) {
            return;
        }
        VideoTaskInfo info = new VideoTaskInfo();
        info.setTaskId(taskId);
        info.setMessage(message);
        info.setCreateTime(DateUtil.now());
        infoMapper.insert(info);
    }

    @Override
    public void error(Long taskId, String errorMsg) {
        if (taskId == null) {
            return;
        }
        VideoTaskError error = new VideoTaskError();
        error.setTaskId(taskId);
        error.setErrorMsg(errorMsg);
        error.setCreateTime(DateUtil.now());
        errorMapper.insert(error);
    }
}

