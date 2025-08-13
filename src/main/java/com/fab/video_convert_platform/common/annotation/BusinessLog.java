package com.fab.video_convert_platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务日志注解
 * 用于标记需要记录业务日志的方法
 * 
 * @author zhanghaorui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLog {

    /**
     * 业务操作描述
     */
    String value() default "";

    /**
     * 业务模块
     */
    String module() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean recordResult() default false;

    /**
     * 是否记录执行时间
     */
    boolean recordTime() default true;
}
