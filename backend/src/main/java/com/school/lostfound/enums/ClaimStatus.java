package com.school.lostfound.enums;

import lombok.Getter;

@Getter
public enum ClaimStatus {
    PENDING(0, "待审核"),
    COMPLETED(1, "已完成"),
    REJECTED(2, "已拒绝"),
    LOCKED(3, "已锁定"),
    APPROVED_WAITING_PICKUP(4, "已通过待取件");

    private final int code;
    private final String description;

    ClaimStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ClaimStatus fromCode(int code) {
        for (ClaimStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ClaimStatus code: " + code);
    }
}
