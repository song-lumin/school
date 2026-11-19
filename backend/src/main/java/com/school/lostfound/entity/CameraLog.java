package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监控调阅申请实体，对应数据库 camera_log 表
 *
 * <p>当用户（多为失主）怀疑物品丢失在某站点附近时，可申请调阅该站点
 * 摄像头监控录像来查找线索。本表记录申请的理由、目标物品、时间范围，
 * 以及管理员审核处理的结果，形成完整的申请-审批流。</p>
 */
@Data
@TableName("camera_log")
public class CameraLog {
    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请调阅哪个收集点的监控（关联 drop_point.id） */
    private Long dropPointId;

    /** 与寻找目标相关的物品/启事 ID（若有） */
    private Long itemId;

    /** 申请理由：说明为什么需要调阅监控（如「书包在服务台附近丢失」） */
    private String applyReason;

    /** 申请人用户 ID */
    private Long applicantId;

    /** 申请调阅的时间范围-开始：精确时间可减少管理员查录像的工作量 */
    private LocalDateTime timeRangeStart;

    /** 申请调阅的时间范围-结束 */
    private LocalDateTime timeRangeEnd;

    /** 申请处理状态（如 0 待处理 / 1 已通过 / 2 已驳回） */
    private Integer status;

    /** 处理结果说明（管理员审核意见、调阅安排等） */
    private String resultNote;

    /** 处理该申请的管理员用户 ID */
    private Long handlerId;

    /** 处理完成时间 */
    private LocalDateTime handledAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
