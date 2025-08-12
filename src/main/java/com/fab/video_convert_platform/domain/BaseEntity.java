package com.fab.video_convert_platform.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Base entity with common identifier and auditing fields.
 * This class is kept pure from persistence framework annotations
 * to avoid coupling the domain model with specific ORM implementations.
 */
@Getter
public abstract class BaseEntity {

    private Long id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
