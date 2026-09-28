package com.school.lostfound.controller;

import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.Result;
import com.school.lostfound.vo.RiskWarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/risk-control")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class RiskControlController {

    private final RiskControlService riskControlService;

    @GetMapping("/warnings")
    public Result<List<RiskWarningVO>> getWarnings() {
        return Result.success(riskControlService.getAllWarnings());
    }

    @GetMapping("/collusion")
    public Result<List<RiskWarningVO>> getCollusionWarnings() {
        return Result.success(riskControlService.detectCollusion());
    }

    @GetMapping("/claim-rate")
    public Result<List<RiskWarningVO>> getClaimRateWarnings() {
        return Result.success(riskControlService.detectAbnormalClaimRate());
    }
}
