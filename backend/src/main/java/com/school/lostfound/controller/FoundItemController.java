package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.vo.FoundItemVO;
import com.school.lostfound.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/found-items")
@RequiredArgsConstructor
public class FoundItemController {

    private final FoundItemService foundItemService;

    @PostMapping
    public Result<FoundItemVO> publish(@Valid @RequestBody FoundItemRequest request,
                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(foundItemService.publish(request, userId));
    }

    @GetMapping
    public Result<IPage<FoundItemVO>> list(ItemQueryRequest request) {
        return Result.success(foundItemService.listPublic(request));
    }

    @GetMapping("/my")
    public Result<IPage<FoundItemVO>> listMy(ItemQueryRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(foundItemService.listMy(userId, request));
    }

    @GetMapping("/{id}")
    public Result<FoundItemVO> getById(@PathVariable Long id) {
        return Result.success(foundItemService.getById(id));
    }

    @PutMapping("/{id}/hand-in")
    public Result<Void> handIn(@PathVariable Long id,
                               @RequestBody Map<String, Long> body,
                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long dropPointId = body.get("dropPointId");
        if (dropPointId == null) {
            return Result.badRequest("站点ID不能为空");
        }
        foundItemService.handIn(id, dropPointId, userId);
        return Result.success("交物成功", null);
    }

    @PutMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.invalidate(id, userId);
        return Result.success("作废成功", null);
    }

    @PutMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        foundItemService.archive(id, userId);
        return Result.success("归档成功", null);
    }
}
