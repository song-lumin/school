package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.service.ClaimService;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public Result<ClaimApplyVO> apply(@Valid @RequestBody ClaimApplyRequest request,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(claimService.apply(request, userId));
    }

    @GetMapping
    public Result<IPage<ClaimApplyVO>> listByItem(@RequestParam Long itemId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(claimService.listByItem(itemId, userId, page, size));
    }

    @GetMapping("/my")
    public Result<IPage<ClaimApplyVO>> listMy(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(claimService.listMy(userId, page, size));
    }

    @GetMapping("/{id}")
    public Result<ClaimApplyVO> getById(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(claimService.getById(id, userId));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        claimService.approve(id, userId);
        return Result.success("已通过，等待取件", null);
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody ClaimRejectRequest request,
                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        claimService.reject(id, request, userId);
        return Result.success("已拒绝", null);
    }

    @PutMapping("/{id}/pickup")
    public Result<ClaimApplyVO> pickup(@PathVariable Long id,
                                       @RequestBody Map<String, String> body,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(claimService.pickup(id, body.get("pickupPhoto"),
                body.get("pickupSignature"), userId));
    }
}
