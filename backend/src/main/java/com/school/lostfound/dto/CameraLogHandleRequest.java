package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 监控调取审批 DTO —— 承接前端"处理监控调取申请"操作的提交内容。
 * <p>业务场景：系统管理员在监控调阅管理页，对某条待处理的调取申请
 * 通过 PUT /api/admin/camera-logs/{id} 做出审批结论，
 * 填写处理说明后留痕，形成完整的监控调阅审计链。
 * <p>为什么需要 DTO：审批操作只需要"结论 + 说明"两个业务字段，
 * 用 DTO 接收可避免前端误传其他字段（如申请人、时间等只读数据）。
 */
@Data
public class CameraLogHandleRequest {
    /** 审批结论：1=已批准，2=已拒绝（0 为待处理，Service 层会拒绝该值） */
    @NotNull private Integer status;
    /** 处理说明（必填，500 字以内），如拒绝原因或调阅情况备注 */
    @NotBlank @Size(max = 500) private String resultNote;
}
