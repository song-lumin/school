package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DropPointRequest {
    @NotBlank(message = "站点名称不能为空")
    private String name;

    @NotBlank(message = "位置描述不能为空")
    private String location;

    private Long adminId;

    private Integer hasCamera;

    private String cameraInfo;
}
