package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("camera_log")
public class CameraLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dropPointId;

    private Long itemId;

    private String applyReason;

    private Long applicantId;

    private LocalDateTime timeRangeStart;

    private LocalDateTime timeRangeEnd;

    private Integer status;

    private String resultNote;

    private Long handlerId;

    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
