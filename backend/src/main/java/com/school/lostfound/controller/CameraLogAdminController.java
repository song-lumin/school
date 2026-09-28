package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.CameraLogCreateRequest;
import com.school.lostfound.dto.CameraLogHandleRequest;
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

    @GetMapping
    public Result<IPage<CameraLogVO>> list(@RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) Long dropPointId,
                                          @RequestParam(required = false) Long itemId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(cameraLogService.list(status, dropPointId, itemId, page, size));
    }

    @PostMapping
    public Result<CameraLogVO> create(@Valid @RequestBody CameraLogCreateRequest request,
                                      Authentication authentication) {
        return Result.success(cameraLogService.create(request, (Long) authentication.getPrincipal()));
    }

    @PutMapping("/{id}")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody CameraLogHandleRequest request,
                               Authentication authentication) {
        cameraLogService.handle(id, request, (Long) authentication.getPrincipal());
        return Result.success("处理完成", null);
    }
}
