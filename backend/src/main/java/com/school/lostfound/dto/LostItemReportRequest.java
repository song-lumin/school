package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 挂失申报请求 DTO —— 用户对某个拾获物品发起"这是我丢的"的认领申报时提交的参数。
 *
 * 业务场景：用户在物品详情页（或"我的失物启事"列表）点击"申报挂失/调取申诉"，
 * 前端将该物品 ID、申报理由及（可选的）证据图片列表 POST 到后端，
 * 由 ClaimApply（认领申请）流程进行审核。
 *
 * 为什么需要它：将前端传入的 JSON 参数集中封装，配合 Jakarta Validation 注解
 * 在 Controller 入口统一做参数校验，避免业务代码中散落的手工判空。
 */
@Data
public class LostItemReportRequest {

    /** 申报针对的物品 ID（必填，对应 FoundItem 表主键） */
    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    /** 申报/申诉原因：描述物品特征、丢失经过等，供管理员审核时判断认领真实性 */
    @NotBlank(message = "调取/申诉原因不能为空")
    private String description;

    /** 证据图片 URL 列表（可选）：如物品照片、购买凭证等，增强认领可信度 */
    private List<String> evidenceImages;
}
