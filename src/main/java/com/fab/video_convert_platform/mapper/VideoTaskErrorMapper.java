package com.fab.video_convert_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.video_convert_platform.domain.VideoTaskError;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for task error logs.
 */
@Mapper
public interface VideoTaskErrorMapper extends BaseMapper<VideoTaskError> {
}

