package com.school.lostfound.controller;

import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.service.DropPointService;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.InventoryItemVO;
import com.school.lostfound.vo.DropPointVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drop-points")
@RequiredArgsConstructor
public class DropPointController {

    private final DropPointService dropPointService;

    @GetMapping
    public Result<List<DropPointVO>> list() {
        return Result.success(dropPointService.listAll());
    }

    @GetMapping("/{id}")
    public Result<DropPointVO> getById(@PathVariable Long id) {
        return Result.success(dropPointService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<DropPointVO> create(@Valid @RequestBody DropPointRequest request) {
        return Result.success(dropPointService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<DropPointVO> update(@PathVariable Long id,
                                      @Valid @RequestBody DropPointRequest request) {
        return Result.success(dropPointService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        dropPointService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<FoundItem>> getPendingItems(@PathVariable Long id) {
        return Result.success(dropPointService.getPendingItems(id));
    }

    @GetMapping("/{id}/pending-pickups")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<ClaimApplyVO>> getPendingPickups(@PathVariable Long id) {
        return Result.success(dropPointService.getPendingPickups(id));
    }

    @PutMapping("/{id}/check")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<Void> checkItem(@PathVariable Long id,
                                  @Valid @RequestBody CheckRequest request,
                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        dropPointService.checkItem(id, request, userId);
        return Result.success("巡检完成，已发放积分", null);
    }

    @GetMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('POINT_ADMIN', 'SYS_ADMIN')")
    public Result<List<InventoryItemVO>> getInventory(@PathVariable Long id) {
        return Result.success(dropPointService.getInventory(id));
    }
}