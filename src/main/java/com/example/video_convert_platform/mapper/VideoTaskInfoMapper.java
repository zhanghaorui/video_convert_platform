package com.example.video_convert_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.video_convert_platform.domain.VideoTaskInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for task info logs.
 */
@Mapper
public interface VideoTaskInfoMapper extends BaseMapper<VideoTaskInfo> {
}

