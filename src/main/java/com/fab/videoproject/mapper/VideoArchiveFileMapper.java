package com.fab.videoproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fab.videoproject.domain.VideoArchiveFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for archived files.
 */
@Mapper
public interface VideoArchiveFileMapper extends BaseMapper<VideoArchiveFile> {
}

