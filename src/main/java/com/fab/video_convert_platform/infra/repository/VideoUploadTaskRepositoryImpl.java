package com.fab.video_convert_platform.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.mapper.VideoUploadTaskMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 视频上传任务仓储实现
 * 基础设施层，负责具体的数据持久化操作
 */
@Repository
public class VideoUploadTaskRepositoryImpl implements VideoUploadTaskRepository {

    private final VideoUploadTaskMapper mapper;

    public VideoUploadTaskRepositoryImpl(VideoUploadTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public VideoUploadTask save(VideoUploadTask task) {
        if (task.getId() == null) {
            mapper.insert(task);
        } else {
            mapper.updateById(task);
        }
        return mapper.selectById(task.getId());
    }

    @Override
    public Optional<VideoUploadTask> findById(Long id) {
        VideoUploadTask task = mapper.selectById(id);
        return Optional.ofNullable(task);
    }

    @Override
    public Optional<VideoUploadTask> findByUuid(String uuid) {
        VideoUploadTask task = mapper.selectOne(
            new LambdaQueryWrapper<VideoUploadTask>()
                .eq(VideoUploadTask::getUuid, uuid)
                .last("LIMIT 1")
        );
        return Optional.ofNullable(task);
    }

    @Override
    public List<VideoUploadTask> findByProjectNo(String projectNo) {
        return mapper.selectList(
            new LambdaQueryWrapper<VideoUploadTask>()
                .eq(VideoUploadTask::getProjectNo, projectNo)
                .orderByDesc(VideoUploadTask::getCreateTime)
        );
    }

    @Override
    public List<VideoUploadTask> findByProjectAndPatientAndStage(String projectNo, String patientCode, String tpStage) {
        return mapper.selectList(
            new LambdaQueryWrapper<VideoUploadTask>()
                .eq(VideoUploadTask::getProjectNo, projectNo)
                .eq(VideoUploadTask::getPatientCode, patientCode)
                .eq(VideoUploadTask::getTpStage, tpStage)
                .orderByDesc(VideoUploadTask::getVersionNo)
        );
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public boolean existsByUuid(String uuid) {
        Long count = mapper.selectCount(
            new LambdaQueryWrapper<VideoUploadTask>()
                .eq(VideoUploadTask::getUuid, uuid)
        );
        return count > 0;
    }
}
