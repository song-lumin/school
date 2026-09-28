package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_log")
public class CreditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer changeAmount;

    private String operationType;

    private Long relatedItemId;

    private Long relatedApplyId;

    private String reason;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
