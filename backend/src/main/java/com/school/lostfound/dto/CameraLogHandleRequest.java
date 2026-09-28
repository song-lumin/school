package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CameraLogHandleRequest {
    @NotNull private Integer status;
    @NotBlank @Size(max = 500) private String resultNote;
}
