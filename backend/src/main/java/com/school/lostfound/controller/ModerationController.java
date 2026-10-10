package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.LostNotice;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.LostNoticeMapper;
import com.school.lostfound.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModerationController {

    private final FoundItemMapper foundItemMapper;
    private final LostNoticeMapper lostNoticeMapper;
    private final DropPointMapper dropPointMapper;

    @GetMapping("/my-takedowns")
    public Result<Map<String, Object>> myTakedowns(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        var items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>().eq(FoundItem::getFounderId, userId)
                        .isNotNull(FoundItem::getTakedownAt)
                        .orderByDesc(FoundItem::getTakedownAt));
        var notices = lostNoticeMapper.selectList(
                new LambdaQueryWrapper<LostNotice>().eq(LostNotice::getPublisherId, userId)
                        .isNotNull(LostNotice::getTakedownAt)
                        .orderByDesc(LostNotice::getTakedownAt));
        return Result.success(Map.of("items", items, "notices", notices));
    }

    // ===== 管理员主动下架 =====

    @PutMapping("/admin/items/{id}/takedown")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','POINT_ADMIN')")
    public Result<Void> takedownItem(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication authentication) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "下架必须填写原因");
        }
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) throw new BusinessException(404, "物品不存在");
        Long currentUserId = (Long) authentication.getPrincipal();
        boolean isSysAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYS_ADMIN"));
        // POINT_ADMIN 只能下架自己管理点位下的物品
        if (!isSysAdmin) {
            if (item.getDropPointId() == null) {
                throw new BusinessException(403, "该物品未关联投放点，无权下架");
            }
            DropPoint point = dropPointMapper.selectById(item.getDropPointId());
            if (point == null || point.getAdminId() == null || !point.getAdminId().equals(currentUserId)) {
                throw new BusinessException(403, "只能下架自己管理点位下的招领帖");
            }
        }
        item.setItemStatus(ItemStatus.VOIDED.getCode());
        item.setTakedownReason((isSysAdmin ? "管理员下架：" : "点位管理员下架：") + reason);
        item.setTakedownAt(LocalDateTime.now());
        item.setTakedownBy(currentUserId);
        foundItemMapper.updateById(item);
        return Result.success("已下架", null);
    }

    @PutMapping("/admin/notices/{id}/takedown")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> takedownNotice(@PathVariable Long id,
                                      @RequestBody Map<String, String> body,
                                      Authentication authentication) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "下架必须填写原因");
        }
        LostNotice notice = lostNoticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "启事不存在");
        notice.setStatus(3);
        notice.setTakedownReason("管理员下架：" + reason);
        notice.setTakedownAt(LocalDateTime.now());
        notice.setTakedownBy((Long) authentication.getPrincipal());
        lostNoticeMapper.updateById(notice);
        return Result.success("已下架", null);
    }

    // ===== 拥有者申诉 =====

    @PostMapping("/items/{id}/appeal")
    public Result<Void> appealItem(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "申诉理由不能为空");
        }
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) throw new BusinessException(404, "物品不存在");
        if (!userId.equals(item.getFounderId()) && !userId.equals(item.getActualFounderId())) {
            throw new BusinessException(403, "只有发布者可以申诉");
        }
        if (item.getAppealStatus() != null && item.getAppealStatus() == 1) {
            throw new BusinessException(409, "已有待审核的申诉");
        }
        item.setAppealStatus(1);
        item.setAppealReason(reason);
        item.setAppealAt(LocalDateTime.now());
        foundItemMapper.updateById(item);
        return Result.success("申诉已提交", null);
    }

    @PostMapping("/notices/{id}/appeal")
    public Result<Void> appealNotice(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "申诉理由不能为空");
        }
        LostNotice notice = lostNoticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "启事不存在");
        if (!userId.equals(notice.getPublisherId())) {
            throw new BusinessException(403, "只有发布者可以申诉");
        }
        if (notice.getAppealStatus() != null && notice.getAppealStatus() == 1) {
            throw new BusinessException(409, "已有待审核的申诉");
        }
        notice.setAppealStatus(1);
        notice.setAppealReason(reason);
        notice.setAppealAt(LocalDateTime.now());
        lostNoticeMapper.updateById(notice);
        return Result.success("申诉已提交", null);
    }

    // ===== 管理员处理申诉 =====

    @GetMapping("/admin/appeals")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Map<String, Object>> listAppeals() {
        var items = foundItemMapper.selectList(
                new LambdaQueryWrapper<FoundItem>().eq(FoundItem::getAppealStatus, 1));
        var notices = lostNoticeMapper.selectList(
                new LambdaQueryWrapper<LostNotice>().eq(LostNotice::getAppealStatus, 1));
        return Result.success(Map.of("items", items, "notices", notices));
    }

    @PutMapping("/admin/items/{id}/appeal")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> handleItemAppeal(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         Authentication authentication) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) throw new BusinessException(404, "物品不存在");
        if (approved) {
            item.setItemStatus(ItemStatus.PUBLIC.getCode());
            item.setAppealStatus(2);
            item.setTakedownReason(null);
        } else {
            item.setAppealStatus(3);
        }
        foundItemMapper.updateById(item);
        return Result.success(approved ? "申诉通过，已恢复公开" : "申诉驳回", null);
    }

    @PutMapping("/admin/notices/{id}/appeal")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public Result<Void> handleNoticeAppeal(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication authentication) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        LostNotice notice = lostNoticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "启事不存在");
        if (approved) {
            notice.setStatus(0);
            notice.setAppealStatus(2);
            notice.setTakedownReason(null);
        } else {
            notice.setAppealStatus(3);
        }
        lostNoticeMapper.updateById(notice);
        return Result.success(approved ? "申诉通过，已恢复公开" : "申诉驳回", null);
    }
}
