package com.school.lostfound.enums;

import lombok.Getter;

@Getter
public enum ItemStatus {
    VOIDED(0, "已作废"),
    PUBLIC(1, "公开待认领"),
    CLAIMING(2, "认领中"),
    PICKED_UP(3, "已取件"),
    ARCHIVED(4, "已归档"),
    EXPIRED(5, "已过期"),
    PUBLISHED_NOT_HANDED_IN(6, "已发布待交物");

    private final int code;
    private final String description;

    ItemStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ItemStatus fromCode(int code) {
        for (ItemStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ItemStatus code: " + code);
    }
}
