package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hand_in_log")
public class HandInLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long dropPointId;

    private Integer handInStatus;

    private LocalDateTime handedInAt;

    private LocalDateTime checkedAt;

    private Long checkedBy;

    private String checkNote;

    private Integer creditIssued;

    private LocalDateTime creditIssuedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
