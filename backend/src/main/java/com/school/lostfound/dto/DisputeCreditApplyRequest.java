package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 系统管理员的积分处置单：
 * EXECUTE 按站点结论执行（后端按类型推导回滚项与物品去处）；
 * ADJUST  按本次勾选的项执行（管理员改主意时用）；
 * REJECT  打回站点重判（不动积分、不动物品）。
 */
@Data
public class DisputeCreditApplyRequest {

    @NotBlank(message = "处置动作不能为空")
    private String action;

    /** ADJUST 时生效：实际要回滚的项 */
    private List<String> rollbackItems;

    /** ADJUST 时生效：物品处置，不传则沿用站点结论 */
    private String itemDispose;

    @NotBlank(message = "处置说明不能为空")
    @Size(max = 1000, message = "处置说明最多 1000 字")
    private String note;
}
