package com.fab.video_convert_platform.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fab.video_convert_platform.domain.VideoArchiveFile;
import com.fab.video_convert_platform.domain.repository.VideoArchiveFileRepository;
import com.fab.video_convert_platform.mapper.VideoArchiveFileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 视频归档文件仓储实现
 * 基础设施层，负责具体的数据持久化操作
 */
@Repository
public class VideoArchiveFileRepositoryImpl implements VideoArchiveFileRepository {

    private final VideoArchiveFileMapper mapper;

    public VideoArchiveFileRepositoryImpl(VideoArchiveFileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public VideoArchiveFile save(VideoArchiveFile archiveFile) {
        if (archiveFile.getId() == null) {
            mapper.insert(archiveFile);
        } else {
            mapper.updateById(archiveFile);
        }
        return mapper.selectById(archiveFile.getId());
    }

    @Override
    public Optional<VideoArchiveFile> findById(Long id) {
        VideoArchiveFile file = mapper.selectById(id);
        return Optional.ofNullable(file);
    }

    @Override
    public List<VideoArchiveFile> findByTaskId(Long taskId) {
        return mapper.selectList(
            new LambdaQueryWrapper<VideoArchiveFile>()
                .eq(VideoArchiveFile::getTaskId, taskId)
                .orderByAsc(VideoArchiveFile::getCreateTime)
        );
    }

    @Override
    public List<VideoArchiveFile> findByTaskIdAndFileType(Long taskId, String fileType) {
        return mapper.selectList(
            new LambdaQueryWrapper<VideoArchiveFile>()
                .eq(VideoArchiveFile::getTaskId, taskId)
                .eq(VideoArchiveFile::getFileType, fileType)
                .orderByAsc(VideoArchiveFile::getCreateTime)
        );
    }

    @Override
    public Optional<VideoArchiveFile> findByTaskIdAndFileTypeAndQuality(Long taskId, String fileType, String qualityLevel) {
        VideoArchiveFile file = mapper.selectOne(
            new LambdaQueryWrapper<VideoArchiveFile>()
                .eq(VideoArchiveFile::getTaskId, taskId)
                .eq(VideoArchiveFile::getFileType, fileType)
                .eq(VideoArchiveFile::getQualityLevel, qualityLevel)
                .last("LIMIT 1")
        );
        return Optional.ofNullable(file);
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public int deleteByTaskId(Long taskId) {
        return mapper.delete(
            new LambdaQueryWrapper<VideoArchiveFile>()
                .eq(VideoArchiveFile::getTaskId, taskId)
        );
    }
}
