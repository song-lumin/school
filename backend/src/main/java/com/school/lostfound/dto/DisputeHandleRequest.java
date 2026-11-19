package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DisputeHandleRequest {

    /** true=通过（申诉成立，按勾选回滚）；false=驳回（不成立，不扣任何人分） */
    @NotNull(message = "处理结果不能为空")
    private Boolean approved;

    /** 仅在旧版「按类型预设结论」路径使用，前端已不再传 */
    private String outcome;

    /**
     * 手动回滚项，管理员勾选本次要撤回的已发放积分：
     * PUBLISH_CHECK=招领方·投放分(+1)、PUBLISH_PICKUP=招领方·认领分(+3)、
     * FORWARD=转发方·转发分(+1)、CLAIMER=认领方·认领分(+1)。
     * 传了该字段（哪怕是空数组）就走手动路径，不再按纠纷类型自动回滚。
     */
    private List<String> rollbackItems;

    /** 物品处置：NONE（不动，默认）/ VOIDED（作废）/ LOST（丢失待查）/ RETURNING（待归还）/ PUBLIC（重新公开） */
    private String itemDispose;

    @NotBlank(message = "处理原因不能为空")
    private String handlerNote;
}
