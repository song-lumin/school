package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimRejectRequest {
    @NotBlank(message = "拒绝理由不能为空")
    private String rejectReason;
}
