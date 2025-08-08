package com.fab.videoproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.videoproject.domain.VideoTaskInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for task info logs.
 */
@Mapper
public interface VideoTaskInfoMapper extends BaseMapper<VideoTaskInfo> {
}

