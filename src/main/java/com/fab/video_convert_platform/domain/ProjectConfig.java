package com.fab.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Project configuration entity mapped to project_config table.
 * @author zhanghaorui
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("project_config")
public class ProjectConfig extends BaseEntity {

    @TableField("project_no")
    private String projectNo;

    @TableField("project_name")
    private String projectName;

    @TableField("archive_root")
    private String archiveRoot;

    /**
     * Callback address for notifying business systems.
     */
    @TableField("callback_url")
    private String callbackUrl;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("ext_json")
    private String extJson;
}

