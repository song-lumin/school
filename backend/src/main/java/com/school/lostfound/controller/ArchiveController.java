package com.school.lostfound.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统留档库：已完结/已下架/超期的物品统一归档，供后期审计、调取、申诉追溯。
 */
@RestController
@RequestMapping("/api/admin/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final FoundItemMapper foundItemMapper;
    private final DropPointMapper dropPointMapper;
    private final UserMapper userMapper;

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'POINT_ADMIN')")
    public Result<List<Map<String, Object>>> listItems(
            @RequestParam(required = false) Long dropPointId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer bizGroup) {

        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        // 留档范围：0作废(含下架)、3已领取、5超期
        wrapper.in(FoundItem::getItemStatus, 0, 3, 5);
        if (dropPointId != null) wrapper.eq(FoundItem::getDropPointId, dropPointId);
        if (category != null && !category.isBlank()) wrapper.eq(FoundItem::getCategory, category);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(FoundItem::getTitle, keyword)
                    .or().like(FoundItem::getDescription, keyword)
                    .or().like(FoundItem::getFoundLocation, keyword));
        }
        if (from != null && !from.isBlank()) wrapper.ge(FoundItem::getPublishedAt, LocalDateTime.parse(from + "T00:00:00"));
        if (to != null && !to.isBlank()) wrapper.le(FoundItem::getPublishedAt, LocalDateTime.parse(to + "T23:59:59"));
        wrapper.orderByDesc(FoundItem::getPublishedAt);

        List<FoundItem> items = foundItemMapper.selectList(wrapper);
        if (items.isEmpty()) return Result.success(List.of());

        Map<Long, String> pointNames = dropPointMapper.selectList(null).stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));

        Set<Long> userIds = new HashSet<>();
        items.forEach(i -> {
            if (i.getFounderId() != null) userIds.add(i.getFounderId());
            if (i.getActualFounderId() != null) userIds.add(i.getActualFounderId());
            if (i.getTakedownBy() != null) userIds.add(i.getTakedownBy());
        });
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        List<Map<String, Object>> result = items.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("title", i.getTitle());
            m.put("category", i.getCategory());
            m.put("images", i.getImages());
            m.put("foundLocation", i.getFoundLocation());
            m.put("publishedAt", i.getPublishedAt());
            m.put("itemStatus", i.getItemStatus());
            m.put("dropPointId", i.getDropPointId());
            m.put("dropPointName", pointNames.get(i.getDropPointId()));
            m.put("founderName", userNames.get(i.getActualFounderId() != null ? i.getActualFounderId() : i.getFounderId()));
            m.put("takedownReason", i.getTakedownReason());
            m.put("takedownAt", i.getTakedownAt());
            m.put("takedownByName", i.getTakedownBy() != null ? userNames.get(i.getTakedownBy()) : null);
            m.put("appealStatus", i.getAppealStatus());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
