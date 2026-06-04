package com.example.video_convert_platform.domain.repository;

import com.example.video_convert_platform.domain.VideoUploadTask;

import java.util.List;
import java.util.Optional;

/**
 * 视频上传任务仓储接口
 * 定义领域层对持久化的抽象，遵循DDD原则
 */
public interface VideoUploadTaskRepository {

    /**
     * 保存视频上传任务
     * @param task 任务实体
     * @return 保存后的任务实体
     */
    VideoUploadTask save(VideoUploadTask task);

    /**
     * 根据ID查找任务
     * @param id 任务ID
     * @return 任务实体（可能为空）
     */
    Optional<VideoUploadTask> findById(Long id);

    /**
     * 根据UUID查找任务
     * @param uuid 任务UUID
     * @return 任务实体（可能为空）
     */
    Optional<VideoUploadTask> findByUuid(String uuid);

    /**
     * 根据项目编号查找所有任务
     * @param projectNo 项目编号
     * @return 任务列表
     */
    List<VideoUploadTask> findByProjectNo(String projectNo);

    /**
     * 根据项目编号、受试者编码、访视点查找任务
     * @param projectNo 项目编号
     * @param patientCode 受试者编码
     * @param tpStage 访视点
     * @return 任务列表
     */
    List<VideoUploadTask> findByProjectAndPatientAndStage(String projectNo, String patientCode, String tpStage);

    /**
     * 根据项目编号、受试者编码、访视点查找任务
     * @param projectNo 项目编号
     * @param patientCode 受试者编码
     * @param visit 访视
     * @return 任务列表
     */
    List<VideoUploadTask> findByProjectAndPatientAndVisit(String projectNo, String patientCode, String visit);

    /**
     * 删除任务
     * @param id 任务ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查UUID是否已存在
     * @param uuid UUID
     * @return 是否存在
     */
    boolean existsByUuid(String uuid);
}
