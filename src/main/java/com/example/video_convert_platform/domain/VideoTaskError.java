package com.example.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Error task log entry stored in video_task_error table.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("video_task_error")
public class VideoTaskError extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("error_msg")
    private String errorMsg;
}

