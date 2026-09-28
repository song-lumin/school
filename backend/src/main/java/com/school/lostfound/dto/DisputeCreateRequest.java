package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DisputeCreateRequest {

    /** 纠纷申诉：ITEM_MISMATCH / OTHER；丢失申诉：ITEM_LOST */
    @NotBlank(message = "工单类型不能为空")
    private String disputeType;

    @NotNull(message = "认领申请ID不能为空")
    private Long applyId;

    @NotBlank(message = "申诉描述不能为空")
    private String description;

    private List<String> evidenceImages;
}
