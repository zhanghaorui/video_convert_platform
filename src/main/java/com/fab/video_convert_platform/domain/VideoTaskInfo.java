package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Normal task log entry stored in video_task_info table.
 */
@Data
@TableName("video_task_info")
public class VideoTaskInfo extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("message")
    private String message;
}

