package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.domain.VideoTaskError;
import com.fab.video_convert_platform.domain.VideoTaskInfo;
import com.fab.video_convert_platform.mapper.VideoTaskErrorMapper;
import com.fab.video_convert_platform.mapper.VideoTaskInfoMapper;
import com.fab.video_convert_platform.service.ITaskLogService;
import com.fab.video_convert_platform.util.DateUtil;
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

