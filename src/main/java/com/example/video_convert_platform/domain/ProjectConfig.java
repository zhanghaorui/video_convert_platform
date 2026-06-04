package com.example.video_convert_platform.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.video_convert_platform.util.DateUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 项目配置领域实体
 * 遵循DDD设计原则，封装项目配置相关的业务逻辑
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
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

    /**
     * 无参构造函数，供MyBatis-Plus使用
     */
    public ProjectConfig() {
    }

    /**
     * 创建新的项目配置
     * 
     * @param projectNo 项目编号
     * @param projectName 项目名称
     * @param archiveRoot 归档根目录
     * @param callbackUrl 回调地址
     * @return 项目配置实体
     */
    public static ProjectConfig create(String projectNo, String projectName, 
                                     String archiveRoot, String callbackUrl) {
        validateCreateParams(projectNo, projectName, archiveRoot);
        
        ProjectConfig config = new ProjectConfig();
        config.projectNo = projectNo;
        config.projectName = projectName;
        config.archiveRoot = archiveRoot;
        config.callbackUrl = callbackUrl;
        config.isActive = true;
        
        LocalDateTime now = DateUtil.now();
        config.setCreateTime(now);
        config.setUpdateTime(now);
        
        return config;
    }

    /**
     * 判断项目是否处于活跃状态
     * 
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    /**
     * 激活项目配置
     * 业务规则：只有非活跃状态的配置才能被激活
     */
    public void activate() {
        if (isActive()) {
            throw new IllegalStateException("项目配置已处于活跃状态");
        }
        
        this.isActive = true;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 停用项目配置
     * 业务规则：只有活跃状态的配置才能被停用
     */
    public void deactivate() {
        if (!isActive()) {
            throw new IllegalStateException("项目配置已处于非活跃状态");
        }
        
        this.isActive = false;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 更新项目信息
     * 
     * @param projectName 项目名称
     * @param archiveRoot 归档根目录
     * @param callbackUrl 回调地址
     */
    public void updateInfo(String projectName, String archiveRoot, String callbackUrl) {
        if (!StringUtils.hasText(projectName)) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (!StringUtils.hasText(archiveRoot)) {
            throw new IllegalArgumentException("归档根目录不能为空");
        }
        
        this.projectName = projectName;
        this.archiveRoot = archiveRoot;
        this.callbackUrl = callbackUrl;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 更新扩展配置
     * 
     * @param extJson 扩展配置JSON
     */
    public void updateExtension(String extJson) {
        this.extJson = extJson;
        this.setUpdateTime(DateUtil.now());
    }

    /**
     * 检查是否配置了回调地址
     * 
     * @return true if callback URL is configured
     */
    public boolean hasCallbackUrl() {
        return StringUtils.hasText(this.callbackUrl);
    }

    /**
     * 校验创建参数
     */
    private static void validateCreateParams(String projectNo, String projectName, String archiveRoot) {
        if (!StringUtils.hasText(projectNo)) {
            throw new IllegalArgumentException("项目编号不能为空");
        }
        if (!StringUtils.hasText(projectName)) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (!StringUtils.hasText(archiveRoot)) {
            throw new IllegalArgumentException("归档根目录不能为空");
        }
    }

    // 为MyBatis-Plus提供必要的setter方法（仅限框架使用）
    public void setId(Long id) {
        super.setId(id);
    }

    public void setCreateTime(LocalDateTime createTime) {
        super.setCreateTime(createTime);
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        super.setUpdateTime(updateTime);
    }
}

