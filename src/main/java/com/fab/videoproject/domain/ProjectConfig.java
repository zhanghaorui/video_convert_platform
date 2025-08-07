package com.fab.videoproject.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Project configuration entity mapped to project_config table.
 */
@Data
@TableName("project_config")
public class ProjectConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectNo;

    private String projectName;

    private String archiveRoot;

    private Boolean isActive;

    private String extJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

