package com.fab.videoproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.videoproject.domain.VideoTaskError;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for task error logs.
 */
@Mapper
public interface VideoTaskErrorMapper extends BaseMapper<VideoTaskError> {
}

