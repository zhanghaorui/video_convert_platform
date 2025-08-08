package com.fab.videoproject.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Normal task log entry stored in video_task_info table.
 */
@Data
@TableName("video_task_info")
public class VideoTaskInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String message;

    private LocalDateTime createTime;
}

