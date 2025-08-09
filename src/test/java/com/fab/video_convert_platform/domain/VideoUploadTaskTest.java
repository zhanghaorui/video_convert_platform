package com.fab.video_convert_platform.domain;

import com.fab.video_convert_platform.domain.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * VideoUploadTask 领域实体单元测试
 */
class VideoUploadTaskTest {

    @Test
    void createOriginalSaved_ShouldCreateTaskWithValidParams() {
        // Given
        String projectNo = "PROJECT001";
        String patientCode = "PATIENT001";
        String tpStage = "TP1";
        String uuid = "test-uuid";
        Integer versionNo = 1;
        String source = "CONTROLLER";
        String fileName = "test.mp4";
        String filePath = "/path/to/test.mp4";
        Long fileSize = 1024L;
        String fileMd5 = "test-md5";

        // When
        VideoUploadTask task = VideoUploadTask.createOriginalSaved(
            projectNo, patientCode, tpStage, uuid, versionNo,
            source, fileName, filePath, fileSize, fileMd5);

        // Then
        assertThat(task.getProjectNo()).isEqualTo(projectNo);
        assertThat(task.getPatientCode()).isEqualTo(patientCode);
        assertThat(task.getTpStage()).isEqualTo(tpStage);
        assertThat(task.getUuid()).isEqualTo(uuid);
        assertThat(task.getVersionNo()).isEqualTo(versionNo);
        assertThat(task.getSource()).isEqualTo(source);
        assertThat(task.getMainFileName()).isEqualTo(fileName);
        assertThat(task.getMainFilePath()).isEqualTo(filePath);
        assertThat(task.getFileSize()).isEqualTo(fileSize);
        assertThat(task.getFileMd5()).isEqualTo(fileMd5);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ORIGINAL_SAVED.name());
        assertThat(task.getCreateTime()).isNotNull();
        assertThat(task.getUpdateTime()).isNotNull();
    }

    @Test
    void createOriginalSaved_ShouldThrowException_WhenProjectNoIsBlank() {
        // When & Then
        assertThatThrownBy(() -> VideoUploadTask.createOriginalSaved(
            "", "PATIENT001", "TP1", "uuid", 1,
            "CONTROLLER", "test.mp4", "/path", 1024L, "md5"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("项目编号不能为空");
    }

    @Test
    void createOriginalSaved_ShouldThrowException_WhenFileSizeIsZero() {
        // When & Then
        assertThatThrownBy(() -> VideoUploadTask.createOriginalSaved(
            "PROJECT001", "PATIENT001", "TP1", "uuid", 1,
            "CONTROLLER", "test.mp4", "/path", 0L, "md5"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("文件大小必须大于0");
    }

    @Test
    void markFinished_ShouldChangeStatusToFinished() {
        // Given
        VideoUploadTask task = createValidTask();

        // When
        task.markFinished();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FINISHED.name());
        assertThat(task.isFinished()).isTrue();
        assertThat(task.isFailed()).isFalse();
    }

    @Test
    void markError_ShouldChangeStatusToFailed() {
        // Given
        VideoUploadTask task = createValidTask();
        String errorMsg = "Processing failed";

        // When
        task.markError(errorMsg);

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED.name());
        assertThat(task.getErrorMsg()).isEqualTo(errorMsg);
        assertThat(task.isFailed()).isTrue();
        assertThat(task.isFinished()).isFalse();
    }

    @Test
    void markError_ShouldThrowException_WhenErrorMessageIsBlank() {
        // Given
        VideoUploadTask task = createValidTask();

        // When & Then
        assertThatThrownBy(() -> task.markError(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("错误信息不能为空");
    }

    @Test
    void markProcessing_ShouldChangeStatusToProcessing() {
        // Given
        VideoUploadTask task = createValidTask();

        // When
        task.markProcessing();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING.name());
        assertThat(task.isProcessing()).isTrue();
    }

    @Test
    void markFinished_ShouldThrowException_WhenCurrentStatusIsNotAllowed() {
        // Given
        VideoUploadTask task = createValidTask();
        task.markError("Some error");

        // When & Then
        assertThatThrownBy(() -> task.markFinished())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("任务状态不允许直接标记为完成");
    }

    private VideoUploadTask createValidTask() {
        return VideoUploadTask.createOriginalSaved(
            "PROJECT001", "PATIENT001", "TP1", "test-uuid", 1,
            "CONTROLLER", "test.mp4", "/path/to/test.mp4", 1024L, "test-md5");
    }
}
