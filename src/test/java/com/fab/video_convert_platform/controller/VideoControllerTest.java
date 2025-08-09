package com.fab.video_convert_platform.controller;

import com.fab.video_convert_platform.common.ApiResponse;
import com.fab.video_convert_platform.common.BusinessException;
import com.fab.video_convert_platform.common.ErrorCode;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.service.IVideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VideoController 单元测试
 */
@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IVideoService videoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadVideo_ShouldReturnSuccess_WhenValidInput() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "test content".getBytes());

        VideoUploadTask mockTask = VideoUploadTask.createOriginalSaved(
            "PROJECT001", "PATIENT001", "TP1", "test-uuid", 1,
            "CONTROLLER", "test.mp4", "/path/test.mp4", 1024L, "test-md5");
        mockTask.setId(1L);

        when(videoService.upload(any(), eq("PROJECT001"), eq("PATIENT001"), eq("TP1")))
            .thenReturn(mockTask);

        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(file)
                .param("projectNo", "PROJECT001")
                .param("patientCode", "PATIENT001")
                .param("tpStage", "TP1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").value(1))
            .andExpect(jsonPath("$.data.projectNo").value("PROJECT001"))
            .andExpect(jsonPath("$.data.patientCode").value("PATIENT001"));

        verify(videoService).upload(any(), eq("PROJECT001"), eq("PATIENT001"), eq("TP1"));
    }

    @Test
    void uploadVideo_ShouldReturnError_WhenProjectNoIsBlank() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "test content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(file)
                .param("projectNo", "")
                .param("patientCode", "PATIENT001")
                .param("tpStage", "TP1"))
            .andExpect(status().isBadRequest());

        verify(videoService, never()).upload(any(), any(), any(), any());
    }

    @Test
    void uploadVideo_ShouldReturnError_WhenServiceThrowsException() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "test content".getBytes());

        when(videoService.upload(any(), any(), any(), any()))
            .thenThrow(new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "Project not found"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(file)
                .param("projectNo", "PROJECT001")
                .param("patientCode", "PATIENT001")
                .param("tpStage", "TP1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void uploadVideoChunk_ShouldReturnSuccess_WhenValidInput() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "chunk content".getBytes());

        doNothing().when(videoService).uploadChunk(any(), eq(0), eq(3), eq("test.mp4"),
            eq("PROJECT001"), eq("PATIENT001"), eq("TP1"), eq("test-uuid"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/videos/upload/chunk")
                .file(file)
                .param("projectNo", "PROJECT001")
                .param("patientCode", "PATIENT001")
                .param("tpStage", "TP1")
                .param("filename", "test.mp4")
                .param("uuid", "test-uuid")
                .param("chunk", "0")
                .param("chunks", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpected(jsonPath("$.data").value("分片上传成功"));

        verify(videoService).uploadChunk(any(), eq(0), eq(3), eq("test.mp4"),
            eq("PROJECT001"), eq("PATIENT001"), eq("TP1"), eq("test-uuid"));
    }

    @Test
    void getTaskStatus_ShouldReturnTask_WhenTaskExists() throws Exception {
        // Given
        VideoUploadTask mockTask = VideoUploadTask.createOriginalSaved(
            "PROJECT001", "PATIENT001", "TP1", "test-uuid", 1,
            "CONTROLLER", "test.mp4", "/path/test.mp4", 1024L, "test-md5");
        mockTask.setId(1L);

        when(videoService.getTaskById(1L)).thenReturn(mockTask);

        // When & Then
        mockMvc.perform(get("/api/v1/videos/tasks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").value(1))
            .andExpect(jsonPath("$.data.projectNo").value("PROJECT001"));

        verify(videoService).getTaskById(1L);
    }

    @Test
    void getTaskStatus_ShouldReturnError_WhenTaskNotFound() throws Exception {
        // Given
        when(videoService.getTaskById(999L))
            .thenThrow(new BusinessException(ErrorCode.TASK_NOT_FOUND, "Task not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/videos/tasks/999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(1701));
    }
}
