package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CameraLogCreateRequest {
    @NotNull private Long dropPointId;
    private Long itemId;
    @NotBlank @Size(max = 500) private String applyReason;
    @NotNull private LocalDateTime timeRangeStart;
    @NotNull private LocalDateTime timeRangeEnd;
}
