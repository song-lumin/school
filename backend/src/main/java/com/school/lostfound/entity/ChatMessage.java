package com.school.lostfound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 纠纷工单：同一工单内「申诉人 · 站点负责人 · 系统管理员」三方共享一条对话 */
    private Long disputeId;

    /** 物品（无工单的物品咨询用，可空） */
    private Long itemId;

    private Long senderId;

    /** USER / POINT_ADMIN / SYS_ADMIN */
    private String senderRole;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
