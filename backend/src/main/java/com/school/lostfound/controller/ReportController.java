package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.ReportCreateRequest;
import com.school.lostfound.dto.ReportHandleRequest;
import com.school.lostfound.service.ReportService;
import com.school.lostfound.vo.ReportVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 用户举报虚假投放 */
    @PostMapping("/reports")
    public Result<ReportVO> create(@Valid @RequestBody ReportCreateRequest request,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(reportService.create(request, userId));
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<IPage<ReportVO>> list(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String reportType,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(reportService.list(status, reportType, page, size));
    }

    @PutMapping("/admin/reports/{id}/handle")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody ReportHandleRequest request,
                               Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        reportService.handle(id, request, operatorId);
        return Result.success("处理完成", null);
    }
}
