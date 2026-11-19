package com.school.lostfound.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Long id;
    private Long disputeId;
    private Long itemId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    /** 当前登录人自己发的，前端靠右显示 */
    private Boolean mine;
    private String content;
    private LocalDateTime createdAt;
}
