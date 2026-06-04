package com.example.video_convert_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志配置属性
 */
@Component
@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {
    
    /**
     * 切面配置
     */
    private final Aspect aspect = new Aspect();
    
    // 防御性 getter 方法
    public Aspect getAspect() {
        return aspect != null ? new Aspect(aspect) : null;
    }

    // Spring Boot 需要的 setter 方法
    public void setAspect(Aspect aspect) {
        if (aspect != null) {
            this.aspect.setEnabled(aspect.isEnabled());
            this.aspect.setSlowMethodThreshold(aspect.getSlowMethodThreshold());
            this.aspect.setLogArgs(aspect.isLogArgs());
            this.aspect.setLogResult(aspect.isLogResult());
        }
    }
    
    public static class Aspect {
        /**
         * 是否启用方法执行日志切面
         */
        private boolean enabled = true;
        
        /**
         * 慢方法阈值(毫秒)
         */
        private long slowMethodThreshold = 3000;
        
        /**
         * 是否记录方法参数
         */
        private boolean logArgs = true;
        
        /**
         * 是否记录方法返回值
         */
        private boolean logResult = false;

        // 拷贝构造器
        public Aspect() {}

        public Aspect(Aspect other) {
            if (other != null) {
                this.enabled = other.enabled;
                this.slowMethodThreshold = other.slowMethodThreshold;
                this.logArgs = other.logArgs;
                this.logResult = other.logResult;
            }
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getSlowMethodThreshold() {
            return slowMethodThreshold;
        }
        
        public void setSlowMethodThreshold(long slowMethodThreshold) {
            this.slowMethodThreshold = slowMethodThreshold;
        }
        
        public boolean isLogArgs() {
            return logArgs;
        }
        
        public void setLogArgs(boolean logArgs) {
            this.logArgs = logArgs;
        }
        
        public boolean isLogResult() {
            return logResult;
        }
        
        public void setLogResult(boolean logResult) {
            this.logResult = logResult;
        }
    }
}
