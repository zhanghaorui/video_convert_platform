package com.example.video_convert_platform.service.impl;

import com.example.video_convert_platform.common.BusinessException;
import com.example.video_convert_platform.common.ErrorCode;
import com.example.video_convert_platform.common.VideoConstants;
import com.example.video_convert_platform.domain.VideoUploadTask;
import com.example.video_convert_platform.domain.repository.ProjectConfigRepository;
import com.example.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.example.video_convert_platform.domain.repository.VideoArchiveFileRepository;
import com.example.video_convert_platform.infra.LocalSliceTaskExecutor;
import com.example.video_convert_platform.infra.NfsService;
import com.example.video_convert_platform.service.TaskLogService;
import com.example.video_convert_platform.service.UploadTaskTxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VideoServiceImpl单元测试
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock
    private ProjectConfigRepository projectConfigRepository;

    @Mock
    private VideoUploadTaskRepository uploadTaskRepository;

    @Mock
    private VideoArchiveFileRepository archiveFileRepository;

    @Mock
    private NfsService nfsService;

    @Mock
    private TaskLogService taskLogService;

    @Mock
    private LocalSliceTaskExecutor sliceTaskExecutor;

    @Mock
    private UploadTaskTxService uploadTaskTxService;

    private VideoServiceImpl videoService;

    @BeforeEach
    void setUp() {
        videoService = new VideoServiceImpl(
                projectConfigRepository,
                uploadTaskRepository,
                archiveFileRepository,
                nfsService,
                taskLogService,
                sliceTaskExecutor,
                null,  // tracer - 不用于getTaskById测试
                uploadTaskTxService
        );
    }

    @Test
    @DisplayName("getTaskById - 正常查询")
    void getTaskById_shouldReturnTask() {
        // Given
        Long taskId = 1L;
        VideoUploadTask task = createMockTask(taskId);
        when(uploadTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // When
        VideoUploadTask result = videoService.getTaskById(taskId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getId());
    }

    @Test
    @DisplayName("getTaskById - 无效ID应抛出异常")
    void getTaskById_shouldThrowException_whenIdInvalid() {
        // When & Then
        assertThrows(BusinessException.class, () -> videoService.getTaskById(null));
        assertThrows(BusinessException.class, () -> videoService.getTaskById(0L));
        assertThrows(BusinessException.class, () -> videoService.getTaskById(-1L));
    }

    @Test
    @DisplayName("getTaskById - 任务不存在应抛出异常")
    void getTaskById_shouldThrowException_whenTaskNotFound() {
        // Given
        when(uploadTaskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> videoService.getTaskById(999L));
        assertEquals(ErrorCode.TASK_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("upload - 空文件应抛出异常")
    void upload_shouldThrowException_whenFileEmpty() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "test.mp4",
                "video/mp4", new byte[0]);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> videoService.upload(emptyFile, "P001", "S001", "V1"));
        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("upload - 文件扩展名不在白名单应抛出异常")
    void upload_shouldThrowException_whenExtensionNotAllowed() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "test.exe",
                "application/octet-stream", new byte[1024]);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> videoService.upload(file, "P001", "S001", "V1"));
        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("upload - 文件大小超过限制应抛出异常")
    void upload_shouldThrowException_whenFileTooLarge() {
        // Given - 创建一个超过500MB的文件
        byte[] largeContent = new byte[(int) (VideoConstants.MAX_FILE_SIZE + 1)];
        MultipartFile largeFile = new MockMultipartFile("file", "test.mp4",
                "video/mp4", largeContent);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> videoService.upload(largeFile, "P001", "S001", "V1"));
        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("upload - 项目不存在应抛出异常")
    void upload_shouldThrowException_whenProjectNotFound() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "test.mp4",
                "video/mp4", new byte[1024]);
        when(projectConfigRepository.findByProjectNo("P001")).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> videoService.upload(file, "P001", "S001", "V1"));
        assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());
    }

    // Helper methods

    private VideoUploadTask createMockTask(Long id) {
        VideoUploadTask task = mock(VideoUploadTask.class);
        when(task.getId()).thenReturn(id);
        when(task.getProjectNo()).thenReturn("P001");
        when(task.getPatientCode()).thenReturn("S001");
        when(task.getTpStage()).thenReturn("V1");
        when(task.getUuid()).thenReturn("uuid123");
        when(task.getVersionNo()).thenReturn(1);
        when(task.getStatus()).thenReturn("FINISHED");
        return task;
    }
}