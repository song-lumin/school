package com.school.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckRequest {
    @NotNull(message = "物品ID不能为空")
    private Long itemId;

    private String checkNote;
}
