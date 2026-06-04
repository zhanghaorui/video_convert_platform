package com.example.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Normal task log entry stored in video_task_info table.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("video_task_info")
public class VideoTaskInfo extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("message")
    private String message;
}

