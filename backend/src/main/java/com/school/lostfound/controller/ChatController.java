package com.school.lostfound.controller;

import com.school.lostfound.dto.ChatSendRequest;
import com.school.lostfound.service.ChatService;
import com.school.lostfound.vo.ChatMessageVO;
import com.school.lostfound.vo.ChatThreadVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纠纷沟通：一张工单一条对话，申诉人 / 该点位负责人 / 系统管理员三方共享。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public Result<List<ChatMessageVO>> list(@RequestParam(required = false) Long disputeId,
                                            @RequestParam(required = false) Long itemId,
                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(chatService.list(disputeId, itemId, userId));
    }

    /** 我的沟通列表（按工单聚合） */
    @GetMapping("/threads")
    public Result<List<ChatThreadVO>> threads(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(chatService.listThreads(userId));
    }

    @PostMapping("/messages")
    public Result<ChatMessageVO> send(@Valid @RequestBody ChatSendRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(chatService.send(request, userId));
    }
}
