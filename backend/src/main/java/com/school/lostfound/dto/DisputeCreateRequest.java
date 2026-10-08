package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DisputeCreateRequest {

    /** ITEM_MISMATCH / OTHER / FALSE_CLAIM */
    @NotBlank(message = "工单类型不能为空")
    private String disputeType;

    /** ITEM_MISMATCH / OTHER 时必填 */
    private Long applyId;

    /** FALSE_CLAIM（物品被冒领）时必填 */
    private Long itemId;

    @NotBlank(message = "申诉描述不能为空")
    private String description;

    private List<String> evidenceImages;
}
