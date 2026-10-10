package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LostItemReportRequest {

    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    @NotBlank(message = "调取/申诉原因不能为空")
    private String description;

    private List<String> evidenceImages;
}
