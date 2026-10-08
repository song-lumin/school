package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.service.DisputeService;
import com.school.lostfound.vo.DisputeVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    /** 纠纷申诉（ITEM_MISMATCH/OTHER 认领人24小时内；FALSE_CLAIM 失主发起，无时间窗口） */
    @PostMapping
    public Result<DisputeVO> createDispute(@Valid @RequestBody DisputeCreateRequest request,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.createDispute(request, userId));
    }

    @GetMapping("/my")
    public Result<IPage<DisputeVO>> listMy(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.listMy(userId, page, size));
    }

    @GetMapping("/{id}")
    public Result<DisputeVO> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(disputeService.getById(id, userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<IPage<DisputeVO>> list(@RequestParam(required = false) String disputeType,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.success(disputeService.list(disputeType, status, page, size));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody DisputeHandleRequest request,
                               Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        disputeService.handle(id, request, operatorId);
        return Result.success("处理完成", null);
    }
}
