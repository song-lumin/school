package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.service.CreditQueryService;
import com.school.lostfound.vo.CertificateVO;
import com.school.lostfound.vo.LeaderboardVO;
import com.school.lostfound.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditQueryService creditQueryService;

    @GetMapping("/me")
    public Result<Integer> getMyCredit(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(creditQueryService.getMyCredit(userId));
    }

    @GetMapping("/logs")
    public Result<IPage<CreditLog>> listMyLogs(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(creditQueryService.listMyLogs(userId, page, size));
    }

    @GetMapping("/leaderboard")
    public Result<LeaderboardVO> getLeaderboard(@RequestParam(defaultValue = "20") int top,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(creditQueryService.getLeaderboard(userId, top));
    }

    @GetMapping("/certificate")
    public Result<CertificateVO> getCertificate(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(creditQueryService.getCertificate(userId));
    }

    @PutMapping("/leaderboard-setting")
    public Result<Void> updateLeaderboardSetting(@RequestBody Map<String, Integer> body,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        creditQueryService.updateLeaderboardSetting(userId, body.get("allowLeaderboard"));
        return Result.success("设置成功", null);
    }
}
