package com.school.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 投放点巡检 DTO —— 承接站点管理员"巡检物品"操作的提交内容。
 * <p>业务场景：站点管理员在站点巡检页，确认某件实物确已交到站点，
 * 通过 PUT /api/drop-points/{id}/check 完成巡检登记，
 * 系统据此给拾得者发放信用积分（CHECK_ISSUE）并记录巡检时间/巡检人。
 * <p>为什么需要 DTO：巡检只针对"某件物品 + 可选备注"，独立成类
 * 便于 @Valid 校验并让接口语义清晰（区别于物品发布、认领等其他操作）。
 */
@Data
public class CheckRequest {
    /** 要巡检的物品 ID（必填），Service 层会校验该物品是否属于当前站点 */
    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    /** 巡检备注（可选），如实物状况、异常情况等，写入 hand_in_log */
    private String checkNote;
}
