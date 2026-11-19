package com.school.lostfound.enums;

import lombok.Getter;

/**
 * 物品状态枚举
 *
 * <p>对应 found_item 表（招领物品）的 status 字段（以整型 code 存储）。
 * 描述一件捡到的物品从登记到最终流转结束的完整生命周期：</p>
 * <pre>
 * PENDING_PLACEMENT(已发布待投放) --站点确认收物上架--> PUBLIC(公开待认领)
 * PUBLIC --有人申请认领--> CLAIMING(认领中)
 * CLAIMING --管理员审核通过并完成取件--> PICKED_UP(已取件)
 * PUBLIC/CLAIMING --到期无人认领--> EXPIRED(已过期) --转交相关部门--> ARCHIVED(已归档)
 * 任意状态 --信息有误/违规则--> VOIDED(已作废)
 * </pre>
 */
@Getter
public enum ItemStatus {
    VOIDED(0, "已作废"),
    PUBLIC(1, "公开待认领"),
    CLAIMING(2, "认领中"),
    PICKED_UP(3, "已取件"),
    ARCHIVED(4, "已归档"),
    EXPIRED(5, "已过期"),
    PENDING_PLACEMENT(6, "已发布待投放");

    /** 存入数据库的整型编码 */
    private final int code;
    /** 状态的中文描述，用于前端展示 */
    private final String description;

    ItemStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据数据库中的整型编码还原为枚举，编码不存在时抛出非法参数异常
     */
    public static ItemStatus fromCode(int code) {
        for (ItemStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ItemStatus code: " + code);
    }
}
