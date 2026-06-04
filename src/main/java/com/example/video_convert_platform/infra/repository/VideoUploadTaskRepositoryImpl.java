package com.example.video_convert_platform.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.video_convert_platform.domain.VideoUploadTask;
import com.example.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.example.video_convert_platform.infra.po.VideoUploadTaskPO;
import com.example.video_convert_platform.mapper.VideoUploadTaskMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        VideoUploadTaskPO po = toPO(task);
        if (po.getId() == null) {
            mapper.insert(po);
            // 将生成的ID设置回原领域实体
            task.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        VideoUploadTaskPO persisted = mapper.selectById(po.getId());
        return toDomain(persisted);
    }

    @Override
    public Optional<VideoUploadTask> findById(Long id) {
        VideoUploadTaskPO po = mapper.selectById(id);
        return Optional.ofNullable(toDomain(po));
    }

    @Override
    public Optional<VideoUploadTask> findByUuid(String uuid) {
        VideoUploadTaskPO po = mapper.selectOne(
            new LambdaQueryWrapper<VideoUploadTaskPO>()
                .eq(VideoUploadTaskPO::getUuid, uuid)
                .last("LIMIT 1")
        );
        return Optional.ofNullable(toDomain(po));
    }

    @Override
    public List<VideoUploadTask> findByProjectNo(String projectNo) {
        List<VideoUploadTaskPO> list = mapper.selectList(
            new LambdaQueryWrapper<VideoUploadTaskPO>()
                .eq(VideoUploadTaskPO::getProjectNo, projectNo)
                .orderByDesc(VideoUploadTaskPO::getCreateTime)
        );
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<VideoUploadTask> findByProjectAndPatientAndStage(String projectNo, String patientCode, String tpStage) {
        List<VideoUploadTaskPO> list = mapper.selectList(
            new LambdaQueryWrapper<VideoUploadTaskPO>()
                .eq(VideoUploadTaskPO::getProjectNo, projectNo)
                .eq(VideoUploadTaskPO::getPatientCode, patientCode)
                .eq(VideoUploadTaskPO::getTpStage, tpStage)
                .orderByDesc(VideoUploadTaskPO::getVersionNo)
        );
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<VideoUploadTask> findByProjectAndPatientAndVisit(String projectNo, String patientCode, String visit) {
        List<VideoUploadTaskPO> list = mapper.selectList(
            new LambdaQueryWrapper<VideoUploadTaskPO>()
                .eq(VideoUploadTaskPO::getProjectNo, projectNo)
                .eq(VideoUploadTaskPO::getPatientCode, patientCode)
                .eq(VideoUploadTaskPO::getVisit, visit)
                .orderByDesc(VideoUploadTaskPO::getVersionNo)
        );
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public boolean existsByUuid(String uuid) {
        Long count = mapper.selectCount(
            new LambdaQueryWrapper<VideoUploadTaskPO>()
                .eq(VideoUploadTaskPO::getUuid, uuid)
        );
        return count > 0;
    }

    private VideoUploadTaskPO toPO(VideoUploadTask task) {
        if (task == null) {
            return null;
        }
        VideoUploadTaskPO po = new VideoUploadTaskPO();
        po.setId(task.getId());
        po.setProjectNo(task.getProjectNo());
        po.setPatientCode(task.getPatientCode());
        po.setTpStage(task.getTpStage());
        po.setVisit(task.getVisit()); // 新增映射
        po.setCheckDate(task.getCheckDate()); // 新增映射
        po.setUuid(task.getUuid());
        po.setVersionNo(task.getVersionNo());
        po.setSource(task.getSource());
        po.setStatus(task.getStatus());
        po.setMainFileName(task.getMainFileName());
        po.setMainFilePath(task.getMainFilePath());
        po.setFileSize(task.getFileSize());
        po.setFileMd5(task.getFileMd5());
        po.setErrorMsg(task.getErrorMsg());
        po.setCreateTime(task.getCreateTime());
        po.setUpdateTime(task.getUpdateTime());
        return po;
    }

    private VideoUploadTask toDomain(VideoUploadTaskPO po) {
        if (po == null) {
            return null;
        }
        return VideoUploadTask.rebuild(
            po.getId(),
            po.getProjectNo(),
            po.getPatientCode(),
            po.getTpStage(),
            po.getUuid(),
            po.getVersionNo(),
            po.getSource(),
            po.getStatus(),
            po.getMainFileName(),
            po.getMainFilePath(),
            po.getFileSize(),
            po.getFileMd5(),
            po.getErrorMsg(),
            po.getCreateTime(),
            po.getUpdateTime(),
            po.getVisit(), // visit
            po.getCheckDate() // checkDate
        );
    }
}
