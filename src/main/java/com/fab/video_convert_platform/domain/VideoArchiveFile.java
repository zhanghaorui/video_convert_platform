package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Archived file entity mapped to video_archive_file table.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("video_archive_file")
public class VideoArchiveFile extends BaseEntity {

    @TableField("task_id")
    private Long taskId;

    @TableField("file_type")
    private String fileType;

    @TableField("quality_level")
    private String qualityLevel;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("play_url")
    private String playUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_md5")
    private String fileMd5;

    /**
     * Processing status, see {@link com.fab.video_convert_platform.domain.enums.ArchiveStatus}.
     */
    @TableField("status")
    private String status;

    @TableField("remark")
    private String remark;
}

