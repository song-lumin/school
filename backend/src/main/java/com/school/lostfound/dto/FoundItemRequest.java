package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FoundItemRequest {
    @NotBlank(message = "物品标题不能为空")
    private String title;

    @NotBlank(message = "物品类别不能为空")
    private String category;

    private String description;

    @NotBlank(message = "拾取地点不能为空")
    private String foundLocation;

    @NotNull(message = "投放点不能为空")
    private Long dropPointId;

    @NotNull(message = "拾取时间不能为空")
    private LocalDateTime foundTime;

    private List<String> images;

    @NotBlank(message = "防伪问题不能为空")
    private String claimQuestion;

    private String referenceAnswer;

    private Integer perishable;

    private Long actualFounderId;
}
