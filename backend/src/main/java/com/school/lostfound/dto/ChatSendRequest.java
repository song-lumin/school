package com.school.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSendRequest {

    /** 纠纷工单号（本轮只开放工单内沟通） */
    private Long disputeId;

    private Long itemId;

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言最多 500 字")
    private String content;
}
