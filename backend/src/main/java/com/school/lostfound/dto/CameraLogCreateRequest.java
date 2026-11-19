package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监控调取申请 DTO —— 承接前端"申请调阅监控"表单的提交内容。
 * <p>业务场景：当物品在投放点遗失或出现认领争议时，
 * 系统管理员通过 POST /api/admin/camera-logs 提交调取申请，
 * 记录"谁、在哪个投放点、因什么原因、调取哪段时间"的监控录像，
 * 形成审批留痕（待处理 -> 已批准/已拒绝），保证监控调阅可追溯。
 * <p>为什么需要 DTO：前端提交的字段与 camera_log 表结构不完全一致，
 * 用 DTO 隔离请求参数，配合 @Valid 在入口层完成校验，避免实体直接暴露。
 */
@Data
public class CameraLogCreateRequest {
    /** 调取的投放点 ID（必填），该站点必须是启用且已配置监控 */
    @NotNull private Long dropPointId;
    /** 关联的物品 ID（可选），便于将调取记录与具体物品对应 */
    private Long itemId;
    /** 调取原因（必填，500 字以内），写入留痕供审批人审核 */
    @NotBlank @Size(max = 500) private String applyReason;
    /** 调取时间段的起点（必填），Service 层会校验不能晚于终点 */
    @NotNull private LocalDateTime timeRangeStart;
    /** 调取时间段的终点（必填） */
    @NotNull private LocalDateTime timeRangeEnd;
}
