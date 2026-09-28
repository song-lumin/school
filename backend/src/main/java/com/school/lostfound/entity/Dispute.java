package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "dispute", autoResultMap = true)
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;

    private Long applyId;

    private Long itemId;

    private String disputeType;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> evidenceImages;

    private Integer status;

    private String handlerNote;

    private Long handlerId;

    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
