package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.entity.SysMessage;
import com.school.lostfound.mapper.SysMessageMapper;
import com.school.lostfound.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final SysMessageMapper messageMapper;

    @Override
    public void push(Long userId, String title, String content, String bizType,
                     Long relatedItemId, Long relatedApplyId) {
        if (userId == null || title == null || content == null) {
            return;
        }
        try {
            SysMessage message = new SysMessage();
            message.setUserId(userId);
            message.setTitle(title.length() > 110 ? title.substring(0, 110) : title);
            message.setContent(content.length() > 1900 ? content.substring(0, 1900) : content);
            message.setBizType(bizType);
            message.setRelatedItemId(relatedItemId);
            message.setRelatedApplyId(relatedApplyId);
            message.setIsRead(0);
            messageMapper.insert(message);
        } catch (Exception e) {
            // 消息失败不应影响主业务（积分、状态流转）
            log.warn("站内消息推送失败：user={}, title={}", userId, title, e);
        }
    }

    @Override
    public IPage<SysMessage> listMy(Long userId, int page, int size) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getCreatedAt)
                .orderByDesc(SysMessage::getId);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public long unreadCount(Long userId) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0);
        Long count = messageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Long id, Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .set(SysMessage::getIsRead, 1)
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getUserId, userId));
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>()
                .set(SysMessage::getIsRead, 1)
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0));
    }
}
