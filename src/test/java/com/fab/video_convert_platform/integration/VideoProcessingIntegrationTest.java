package com.fab.video_convert_platform.integration;

import com.fab.video_convert_platform.domain.ProjectConfig;
import com.fab.video_convert_platform.domain.VideoUploadTask;
import com.fab.video_convert_platform.domain.repository.VideoUploadTaskRepository;
import com.fab.video_convert_platform.mapper.ProjectConfigMapper;
import com.fab.video_convert_platform.service.IVideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 视频处理系统集成测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class VideoProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IVideoService videoService;

    @Autowired
    private ProjectConfigMapper projectConfigMapper;

    @Autowired
    private VideoUploadTaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectConfig testProject;

    @BeforeEach
    void setUp() {
        // 创建测试项目配置
        testProject = new ProjectConfig();
        testProject.setProjectNo("TEST_PROJECT");
        testProject.setProjectName("测试项目");
        testProject.setArchiveRoot("/tmp/test-archive");
        testProject.setIsActive(true);
        projectConfigMapper.insert(testProject);
    }

    @Test
    void completeVideoUploadFlow_ShouldWork() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "integration-test.mp4",
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "test video content for integration".getBytes());

        // When - 上传视频
        String response = mockMvc.perform(multipart("/api/v1/videos/upload")
                .file(file)
                .param("projectNo", "TEST_PROJECT")
                .param("patientCode", "PATIENT_IT_001")
                .param("tpStage", "BASELINE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Then - 验证任务创建
        var responseMap = objectMapper.readValue(response, Map.class);
        Long taskId = ((Number) ((Map) responseMap.get("data")).get("taskId")).longValue();

        VideoUploadTask task = taskRepository.findById(taskId).orElse(null);
        assertThat(task).isNotNull();
        assertThat(task.getProjectNo()).isEqualTo("TEST_PROJECT");
        assertThat(task.getPatientCode()).isEqualTo("PATIENT_IT_001");
        assertThat(task.getTpStage()).isEqualTo("BASELINE");

        // When - 查询任务状态
        mockMvc.perform(get("/api/v1/videos/tasks/" + taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").value(taskId.intValue()))
            .andExpect(jsonPath("$.data.projectNo").value("TEST_PROJECT"));
    }

    @Test
    void monitoringEndpoints_ShouldReturnMetrics() throws Exception {
        // When & Then - 系统概览
        mockMvc.perform(get("/api/v1/monitor/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.activeTasks").exists())
            .andExpect(jsonPath("$.data.systemStatus").exists());

        // When & Then - 性能指标
        mockMvc.perform(get("/api/v1/monitor/performance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memory").exists())
            .andExpect(jsonPath("$.data.activeTasks").exists());

        // When & Then - 项目统计
        mockMvc.perform(get("/api/v1/monitor/project-stats")
                .param("projectNo", "TEST_PROJECT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectNo").value("TEST_PROJECT"));
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists());
    }
}
