package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Error task log entry stored in video_task_error table.
 */
@Data
@TableName("video_task_error")
public class VideoTaskError {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String errorMsg;

    private LocalDateTime createTime;
}

