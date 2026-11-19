package com.school.lostfound.enums;

import lombok.Getter;

/**
 * 认领申请状态枚举
 *
 * <p>对应 claim_apply 表的 status 字段（以整型 code 存储）。描述一条
 * 「用户对某招领物品发起的认领申请」当前所处的流程节点：</p>
 * <pre>
 * PENDING(待审核) --站点管理员通过--> APPROVED_WAITING_PICKUP(已通过待取件) --凭码取件核销--> COMPLETED(已完成)
 * PENDING(待审核) --管理员驳回--> REJECTED(已拒绝)
 * APPROVED_WAITING_PICKUP --超时未取件等特殊情况--> LOCKED(已锁定)
 * </pre>
 */
@Getter
public enum ClaimStatus {
    PENDING(0, "待审核"),
    COMPLETED(1, "已完成"),
    REJECTED(2, "已拒绝"),
    LOCKED(3, "已锁定"),
    APPROVED_WAITING_PICKUP(4, "已通过待取件");

    /** 存入数据库的整型编码 */
    private final int code;
    /** 状态的中文描述，用于前端展示 */
    private final String description;

    ClaimStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据数据库中的整型编码还原为枚举，编码不存在时抛出非法参数异常
     */
    public static ClaimStatus fromCode(int code) {
        for (ClaimStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ClaimStatus code: " + code);
    }
}
