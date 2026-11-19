package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 争议/仲裁实体，对应数据库 dispute 表
 *
 * <p>当用户对认领审核结果不服（如申请被误拒）或对取件结果有异议时，
 * 可发起争议申请，由系统管理员仲裁。本表记录争议的类型、描述、
 * 证据截图，以及管理员的仲裁结论，是申诉-仲裁流程的核心表。</p>
 *
 * <p>autoResultMap = true：evidenceImages 以 JSON 存储，需要自动
 * resultMap 完成 List 与数据库字段的互转。</p>
 */
@Data
@TableName(value = "dispute", autoResultMap = true)
public class Dispute {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起争议的用户 ID */
    private Long applicantId;

    /** 关联的认领申请 ID（多数争议源于认领被拒或认领结果异议） */
    private Long applyId;

    /** 关联的物品 ID */
    private Long itemId;

    /** 争议类型（如 认领被误拒 / 物品状况不符 / 信用分扣除异议） */
    private String disputeType;

    /** 争议的详细描述 */
    private String description;

    /** 证据图片 URL 列表（截图、照片等），数据库以 JSON 存储 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceImages;

    /** 争议处理状态（如 0 待处理 / 1 已处理） */
    private Integer status;

    /** 管理员的仲裁意见/处理说明 */
    private String handlerNote;

    /** 仲裁管理员用户 ID */
    private Long handlerId;

    /** 仲裁处理时间 */
    private LocalDateTime handledAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
