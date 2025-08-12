package com.fab.video_convert_platform.config.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 目录路径校验注解
 * 校验路径是否为有效的目录路径
 * 
 * @author zhanghaorui
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DirectoryPathValidator.class)
public @interface ValidDirectoryPath {

    String message() default "目录路径无效或不可访问";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 如果目录不存在是否自动创建
     */
    boolean createIfNotExists() default false;
}
