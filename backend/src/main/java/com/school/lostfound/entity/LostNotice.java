package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "lost_notice", autoResultMap = true)
public class LostNotice {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String description;

    private String lostLocation;

    private LocalDateTime lostTime;

    private String contactInfo;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Integer status;

    private Long publisherId;

    private Long matchedItemId;

    private String takedownReason;

    private LocalDateTime takedownAt;

    private Long takedownBy;

    private Integer appealStatus;

    private String appealReason;

    private LocalDateTime appealAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
