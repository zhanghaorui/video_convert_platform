package com.fab.video_convert_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.video_convert_platform.infra.po.VideoUploadTaskPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for video_upload_task table.
 */
@Mapper
public interface VideoUploadTaskMapper extends BaseMapper<VideoUploadTaskPO> {
}
