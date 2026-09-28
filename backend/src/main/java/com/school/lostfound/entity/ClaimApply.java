package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim_apply")
public class ClaimApply {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long sourceNoticeId;

    private Long claimerId;

    private String answer;

    private Integer applyStatus;

    private String rejectReason;

    private Integer rejectCount;

    private Integer confidenceScore;

    private Integer lowConfidence;

    private String confidenceReason;

    private LocalDateTime pickupTime;

    private String pickupPhoto;

    private String pickupSignature;

    private Integer creditIssued;

    private Integer creditRollback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
