package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.lostfound.dto.LostNoticeRequest;
import com.school.lostfound.dto.ForwardClaimRequest;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.service.impl.ImageSearchServiceImpl;
import com.school.lostfound.service.LostNoticeService;
import com.school.lostfound.vo.LostNoticeVO;
import com.school.lostfound.vo.MatchResultVO;
import com.school.lostfound.vo.Result;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.ImageSearchResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lost-notices")
@RequiredArgsConstructor
public class LostNoticeController {

    private final LostNoticeService lostNoticeService;
    private final FoundItemService foundItemService;
    private final ImageSearchServiceImpl imageSearchService;

    @PostMapping
    public Result<LostNoticeVO> publish(@Valid @RequestBody LostNoticeRequest request,
                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(lostNoticeService.publish(request, userId));
    }

    @GetMapping
    public Result<IPage<LostNoticeVO>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(lostNoticeService.list(keyword, category, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<LostNoticeVO> getById(@PathVariable Long id) {
        return Result.success(lostNoticeService.getById(id));
    }

    @PutMapping("/{id}/close")
    public Result<LostNoticeVO> close(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(lostNoticeService.close(id, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> deleteByAdmin(@PathVariable Long id) {
        lostNoticeService.deleteByAdmin(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/matches")
    public Result<List<FoundItem>> smartMatch(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(lostNoticeService.smartMatch(id, userId));
    }

    /** Sprint2 智能匹配：时间±3天 + 地点相似 + 关键词加权评分 */
    @GetMapping("/{id}/matches/v2")
    public Result<List<MatchResultVO>> smartMatchV2(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(lostNoticeService.smartMatchV2(id, userId));
    }

    @PostMapping("/{id}/forward")
    public Result<ClaimApplyVO> forward(@PathVariable Long id,
                                                                @Validated @RequestBody ForwardClaimRequest request,
                                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(lostNoticeService.forward(id, request, userId));
    }

    @PostMapping(value = "/search-by-image", consumes = "multipart/form-data")
    public Result<List<ImageSearchResultVO>> searchByImage(@RequestPart("image") MultipartFile image,
                                                           @RequestParam(required = false) String category) {
        return Result.success(imageSearchService.searchByImage(image, category));
    }

}
