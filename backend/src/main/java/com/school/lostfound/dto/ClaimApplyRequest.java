package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 认领申请 DTO —— 承接前端"申请认领物品"表单的提交内容。
 * <p>业务场景：失主在物品详情页点击"认领"后，需回答发布者预设的
 * 防伪问题，通过 POST /api/claims 提交申请，进入待审核状态，
 * 由发布者比对参考答案决定通过/驳回（错 3 次锁定 24 小时）。
 * <p>为什么需要 DTO：认领操作只依赖"认领哪个物品 + 防伪答案"，
 * 独立 DTO 可隔离 claim_apply 表结构，并在入口层拦截空答案。
 */
@Data
public class ClaimApplyRequest {
    /** 要认领的物品 ID（必填），必须是公开(PUBLIC)状态且非本人发布 */
    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    /** 防伪问题的回答（必填），与发布者预设的参考答案比对做人工审核 */
    @NotBlank(message = "问题答案不能为空")
    private String answer;
}
