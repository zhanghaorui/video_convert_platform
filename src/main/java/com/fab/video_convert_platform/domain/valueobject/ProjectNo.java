package com.fab.video_convert_platform.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 项目编号值对象
 * 封装项目编号的业务规则和验证逻辑
 */
@Getter
@ToString
@EqualsAndHashCode
public class ProjectNo {
    
    private final String value;

    /**
     * 私有构造函数
     * @param value 项目编号值
     */
    private ProjectNo(String value) {
        this.value = value;
    }

    /**
     * 创建项目编号值对象
     * @param value 项目编号字符串
     * @return 项目编号值对象
     * @throws IllegalArgumentException 如果项目编号格式不正确
     */
    public static ProjectNo of(String value) {
        validateProjectNo(value);
        return new ProjectNo(value);
    }

    /**
     * 验证项目编号格式
     * 业务规则：项目编号必须是6-32位的字母数字组合，允许下划线和中划线
     * @param value 待验证的项目编号
     */
    private static void validateProjectNo(String value) {
        if (Objects.isNull(value) || value.trim().isEmpty()) {
            throw new IllegalArgumentException("项目编号不能为空");
        }
        
        String trimmed = value.trim();
        if (trimmed.length() < 6 || trimmed.length() > 32) {
            throw new IllegalArgumentException("项目编号长度必须在6-32位之间");
        }
        
        if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("项目编号只能包含字母、数字、下划线和中划线");
        }
    }

    /**
     * 获取项目编号字符串值
     * @return 项目编号字符串
     */
    public String getValue() {
        return value;
    }

    /**
     * 检查是否为测试项目
     * 业务规则：以TEST_开头的项目编号为测试项目
     * @return 是否为测试项目
     */
    public boolean isTestProject() {
        return value.toUpperCase().startsWith("TEST_");
    }

    /**
     * 检查是否为生产项目
     * 业务规则：以PROD_开头的项目编号为生产项目
     * @return 是否为生产项目
     */
    public boolean isProdProject() {
        return value.toUpperCase().startsWith("PROD_");
    }
}
