package com.school.lostfound.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("普通用户"),
    POINT_ADMIN("站点管理员"),
    SYS_ADMIN("系统管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
