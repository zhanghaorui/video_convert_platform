package com.fab.video_convert_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for upload tasks.
 */
@Mapper
public interface VideoUploadTaskMapper extends BaseMapper<VideoUploadTask> {
}
