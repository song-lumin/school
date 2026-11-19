package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物品上交流水实体，对应数据库 hand_in_log 表
 *
 * <p>记录「捡拾者把捡到的物品交到失物收集点」这条业务线：
 * 捡拾者登记上交 → 站点管理员确认签收（核对物品）→ 发放信用分奖励。
 * 每条记录都是一次上交行为及其签收进度的审计凭证。</p>
 */
@Data
@TableName("hand_in_log")
public class HandInLog {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上交的物品 ID（关联 found_item.id） */
    private Long itemId;

    /** 上交到的收集点 ID（关联 drop_point.id） */
    private Long dropPointId;

    /** 上交状态（如 待确认 / 已确认签收） */
    private Integer handInStatus;

    /** 上交时间 */
    private LocalDateTime handedInAt;

    /** 管理员确认签收时间 */
    private LocalDateTime checkedAt;

    /** 执行签收的管理员用户 ID */
    private Long checkedBy;

    /** 签收备注（如物品实际状况与登记不符时的说明） */
    private String checkNote;

    /** 是否已给捡拾者发放信用分（0/1），防止重复发放 */
    private Integer creditIssued;

    /** 信用分发放时间 */
    private LocalDateTime creditIssuedAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
