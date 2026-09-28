package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "found_item", autoResultMap = true)
public class FoundItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String description;

    private String foundLocation;

    private LocalDateTime foundTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String claimQuestion;

    private Integer perishable;

    private Integer itemStatus;

    private Long founderId;

    private Long actualFounderId;

    private Long dropPointId;

    private LocalDateTime publishedAt;

    private LocalDateTime claimedAt;

    private Integer expireWarningSent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
