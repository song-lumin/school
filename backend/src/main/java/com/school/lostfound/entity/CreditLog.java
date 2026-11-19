package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信用分流水实体，对应数据库 credit_log 表
 *
 * <p>记录每一次信用分的变动明细（加分/扣分、原因、关联业务）。
 * user.credit_score 只存当前总分，本表是唯一的变动审计依据，
 * 争议仲裁时据此追溯「分数是怎么扣的」，也支撑排行榜与信用明细页展示。</p>
 */
@Data
@TableName("credit_log")
public class CreditLog {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分数变动的目标用户 ID */
    private Long userId;

    /** 变动数额：正数加分、负数扣分（如 +5 捡到物品上交、-10 恶意认领） */
    private Integer changeAmount;

    /** 操作类型（如 HAND_IN 上交、CLAIM_SUCCESS 认领完成、PENALTY 处罚） */
    private String operationType;

    /** 关联的物品 ID（若有），便于从流水跳转到具体物品 */
    private Long relatedItemId;

    /** 关联的认领申请 ID（若有） */
    private Long relatedApplyId;

    /** 变动原因的中文说明 */
    private String reason;

    /** 操作人用户 ID：系统自动触发时可为空，人工调整时记录管理员 */
    private Long operatorId;

    /** 流水产生时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
