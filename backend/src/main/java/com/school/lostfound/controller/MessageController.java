package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.SysMessage;
import com.school.lostfound.service.MessageService;
import com.school.lostfound.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<IPage<SysMessage>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(messageService.listMy(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(Map.of("count", messageService.unreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        messageService.markRead(id, userId);
        return Result.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        messageService.markAllRead(userId);
        return Result.success("全部已读", null);
    }
}
