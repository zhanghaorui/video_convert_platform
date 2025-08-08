package com.fab.video_convert_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for archived files.
 */
@Mapper
public interface VideoArchiveFileMapper extends BaseMapper<VideoArchiveFile> {
}

