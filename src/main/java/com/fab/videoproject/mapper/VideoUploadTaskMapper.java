package com.fab.videoproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.videoproject.domain.VideoUploadTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for upload tasks.
 */
@Mapper
public interface VideoUploadTaskMapper extends BaseMapper<VideoUploadTask> {
}
