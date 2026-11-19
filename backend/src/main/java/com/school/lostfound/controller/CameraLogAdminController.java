package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.service.CameraLogService;
import com.school.lostfound.vo.CameraLogVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/camera-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYS_ADMIN')")
public class CameraLogAdminController {
    private final CameraLogService cameraLogService;
    private final DropPointMapper dropPointMapper;

    @GetMapping
    public Result<IPage<CameraLogVO>> list(@RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Long dropPointId,
                                          @RequestParam(required = false) Long itemId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(cameraLogService.list(status, dropPointId, itemId, page, size));
    }

    /** 站点管理员：本人负责点位相关的调取申请与审批进度（只读） */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','POINT_ADMIN')")
    public Result<IPage<CameraLogVO>> mine(@RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        return Result.success(cameraLogService.listMine(operatorId, status, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','POINT_ADMIN')")
    public Result<CameraLogVO> create(@Valid @RequestBody CameraLogCreateRequest request,
                                      Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        boolean isSysAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYS_ADMIN"));
        // 站点管理员只能为本点位发起调取申请；审批仍由系统管理员完成
        if (!isSysAdmin) {
            DropPoint point = dropPointMapper.selectById(request.getDropPointId());
            if (point == null || point.getAdminId() == null || !point.getAdminId().equals(operatorId)) {
                throw new BusinessException(403, "只能为本点位发起监控调取申请");
            }
        }
        return Result.success(cameraLogService.create(request, operatorId));
    }

    @PutMapping("/{id}")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody CameraLogHandleRequest request,
                               Authentication authentication) {
        cameraLogService.handle(id, request, (Long) authentication.getPrincipal());
        return Result.success("处理完成", null);
    }
}
