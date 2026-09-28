package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LostNoticeRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "物品类别不能为空")
    private String category;

    private String description;

    @NotBlank(message = "丢失地点不能为空")
    private String lostLocation;

    @NotNull(message = "丢失时间不能为空")
    private LocalDateTime lostTime;

    @NotBlank(message = "联系方式不能为空")
    private String contactInfo;

    private List<String> images;
}
