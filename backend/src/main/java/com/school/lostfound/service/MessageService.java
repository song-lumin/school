package com.school.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.SysMessage;

public interface MessageService {

    /** 推送一条站内消息（接收人为空或管理员时自动跳过不报错） */
    void push(Long userId, String title, String content, String bizType, Long relatedItemId, Long relatedApplyId);

    IPage<SysMessage> listMy(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long id, Long userId);

    void markAllRead(Long userId);
}
