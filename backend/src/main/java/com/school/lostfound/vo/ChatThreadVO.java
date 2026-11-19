package com.school.lostfound.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 某个工单的沟通摘要，用于「沟通处」列表 */
@Data
public class ChatThreadVO {
    private Long disputeId;
    private Long itemId;
    private String itemTitle;
    private String disputeType;
    private Integer status;
    private String lastContent;
    private String lastSender;
    private String lastSenderRole;
    private LocalDateTime lastAt;
    /** 该工单还没人留言时为 false */
    private Boolean hasMessage;
}
