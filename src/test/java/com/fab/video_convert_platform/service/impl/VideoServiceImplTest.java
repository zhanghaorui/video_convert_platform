package com.fab.video_convert_platform.service.impl;

import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.common.VideoConstants;
import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.service.VideoTaskDomainService;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import com.fab.video_convert_platform.mapper.VideoUploadTaskMapper;
import com.fab.video_convert_platform.service.IArchiveService;
import com.fab.video_convert_platform.service.ITaskLogService;
import com.fab.video_convert_platform.infra.NfsService;
import com.fab.video_convert_platform.util.DigestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VideoServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock
    private ProjectConfigMapper projectConfigMapper;

    @Mock
    private VideoUploadTaskMapper uploadTaskMapper;

    @Mock
    private IArchiveService archiveService;

    @Mock
    private NfsService nfsService;

    @Mock
    private ITaskLogService taskLogService;

    @Mock
    private VideoTaskDomainService videoTaskDomainService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private VideoServiceImpl videoService;

    private ProjectConfig mockConfig;
    private VideoUploadTask mockTask;

    @BeforeEach
    void setUp() {
        mockConfig = new ProjectConfig();
        mockConfig.setProjectNo("PROJECT001");
        mockConfig.setArchiveRoot("/archive/root");

        mockTask = VideoUploadTask.createOriginalSaved(
            "PROJECT001", "PATIENT001", "TP1", "test-uuid", 1,
            VideoConstants.SOURCE_CONTROLLER, "test.mp4", "/path/test.mp4", 1024L, "test-md5");
        mockTask.setId(1L);
    }

    @Test
    void upload_ShouldReturnTask_WhenValidInput() throws IOException, InterruptedException {
        // Given
        when(projectConfigMapper.selectOne(any())).thenReturn(mockConfig);
        when(mockFile.getOriginalFilename()).thenReturn("test.mp4");
        when(mockFile.getSize()).thenReturn(1024L);
        when(uploadTaskMapper.selectById(1L)).thenReturn(mockTask);

        try (MockedStatic<DigestUtil> digestUtil = mockStatic(DigestUtil.class)) {
            digestUtil.when(() -> DigestUtil.md5(any(Path.class))).thenReturn("test-md5");

            // When
            VideoUploadTask result = videoService.upload(mockFile, "PROJECT001", "PATIENT001", "TP1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProjectNo()).isEqualTo("PROJECT001");
            verify(nfsService).saveFile(eq(mockFile), any(Path.class));
            verify(taskLogService).info(anyLong(), eq("original file archived"));
            verify(videoTaskDomainService).processSlices(eq(mockConfig), any(VideoUploadTask.class));
        }
    }

    @Test
    void upload_ShouldThrowException_WhenProjectNotFound() {
        // Given
        when(projectConfigMapper.selectOne(any())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> videoService.upload(mockFile, "INVALID_PROJECT", "PATIENT001", "TP1"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    void upload_ShouldThrowException_WhenNfsServiceFails() throws IOException {
        // Given
        when(projectConfigMapper.selectOne(any())).thenReturn(mockConfig);
        when(mockFile.getOriginalFilename()).thenReturn("test.mp4");
        when(mockFile.getSize()).thenReturn(1024L);
        doThrow(new IOException("NFS error")).when(nfsService).saveFile(any(), any());

        // When & Then
        assertThatThrownBy(() -> videoService.upload(mockFile, "PROJECT001", "PATIENT001", "TP1"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORE_FILE_FAILED);
    }

    @Test
    void getTaskById_ShouldReturnTask_WhenTaskExists() {
        // Given
        when(uploadTaskMapper.selectById(1L)).thenReturn(mockTask);

        // When
        VideoUploadTask result = videoService.getTaskById(1L);

        // Then
        assertThat(result).isEqualTo(mockTask);
    }

    @Test
    void getTaskById_ShouldThrowException_WhenTaskNotExists() {
        // Given
        when(uploadTaskMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> videoService.getTaskById(1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    void getTaskById_ShouldThrowException_WhenTaskIdIsInvalid() {
        // When & Then
        assertThatThrownBy(() -> videoService.getTaskById(null))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PARAM_ERROR);

        assertThatThrownBy(() -> videoService.getTaskById(0L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PARAM_ERROR);
    }
}
