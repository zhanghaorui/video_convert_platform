package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Archived file entity mapped to video_archive_file table.
 */
@Data
@TableName("video_archive_file")
public class VideoArchiveFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String fileType;

    private String qualityLevel;

    private String fileName;

    private String filePath;

    private String playUrl;

    private Long fileSize;

    private String fileMd5;

    /**
     * Processing status, see {@link com.fab.video_convert_platform.domain.enums.ArchiveStatus}.
     */
    private String status;

    private LocalDateTime createTime;

    private String remark;
}

