package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报记录实体类，对应数据库表 report_log。
 *
 * <p>业务场景：用户可以对其他用户发布的失物招领物品（found_item）或寻物启事（lost_notice）
 * 发起举报，例如"虚假发布"（FAKE_PUBLISH）、"描述与实物不符"（DESC_MISMATCH）或"其他"（OTHER）。
 * 管理员在后台查看待处理的举报，核实后判定举报有效或无效；判定为有效的举报会对被举报人
 * 扣除信用分，举报人也可能获得奖励，从而形成社区自治的诚信机制。</p>
 *
 * <p>为什么需要它：举报必须留痕——谁举报的、举报了什么、附了什么证据、管理员如何处理的，
 * 这些信息既是审核依据，也是事后追溯的凭证，因此单独建一张日志表存储完整生命周期。</p>
 *
 * <p>使用方：ReportServiceImpl（创建举报、管理员处理举报）、FoundItemServiceImpl（物品被删/失效时联动清理举报）。</p>
 */
@Data
@TableName(value = "report_log", autoResultMap = true)
public class ReportLog {
    /** 主键，数据库自增（AUTO） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人的用户 ID，关联 user 表的 id */
    private Long reporterId;

    /** 被举报物品的 ID（targetType 为 ITEM 时有值），关联 found_item 表的 id */
    private Long itemId;

    /** 举报目标类型：ITEM（物品）或 NOTICE（寻物启事） */
    private String targetType;

    /** 被举报寻物启事的 ID（targetType 为 NOTICE 时有值），关联 lost_notice 表的 id */
    private Long noticeId;

    /** 举报类型：FAKE_PUBLISH（虚假发布）/ DESC_MISMATCH（描述不符）/ OTHER（其他） */
    private String reportType;

    /** 举报人填写的详细描述 */
    private String description;

    /** 举报证据图片 URL 列表，以 JSON 格式存库，通过 JacksonTypeHandler 与 List 互转 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceImages;

    /** 处理状态：0-待处理，1-举报有效，2-举报无效 */
    private Integer status;

    /** 管理员处理时填写的备注（如判定理由） */
    private String handlerNote;

    /** 处理该举报的管理员用户 ID */
    private Long handlerId;

    /** 处理完成时间 */
    private LocalDateTime handledAt;

    /** 记录创建时间（即举报提交时间），插入时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间，插入和更新时由 MyMetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
