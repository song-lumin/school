package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.FoundItemRequest;
import com.school.lostfound.dto.ItemQueryRequest;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.FoundItemService;
import com.school.lostfound.service.ImageFingerprintService;
import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.FoundItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoundItemServiceImpl implements FoundItemService {

    private static final int MAX_WEEKLY_PUBLISH = 3;
    private static final int NEW_USER_DAYS = 7;

    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;
    private final UserMapper userMapper;
    private final DropPointMapper dropPointMapper;
    private final RiskControlService riskControlService;
    private final ImageFingerprintService imageFingerprintService;

    @Override
    @Transactional
    public FoundItemVO publish(FoundItemRequest request, Long currentUserId) {
        checkWeeklyLimit(currentUserId);

        if (riskControlService.containsSensitiveWord(request.getTitle())
                || riskControlService.containsSensitiveWord(request.getDescription())) {
            throw new BusinessException(400, "内容包含敏感词，请修改后重新发布");
        }

        User founder = userMapper.selectById(currentUserId);
        if (founder == null) {
            throw new BusinessException(404, "用户不存在");
        }

        FoundItem item = new FoundItem();
        item.setTitle(request.getTitle());
        item.setCategory(request.getCategory());
        item.setDescription(request.getDescription());
        item.setFoundLocation(request.getFoundLocation());
        item.setFoundTime(request.getFoundTime());
        item.setImages(request.getImages());
        item.setClaimQuestion(request.getClaimQuestion());
        item.setPerishable(request.getPerishable() != null ? request.getPerishable() : 0);
        item.setItemStatus(ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode());
        item.setFounderId(currentUserId);

        if (request.getActualFounderId() != null && !request.getActualFounderId().equals(currentUserId)) {
            User actualFounder = userMapper.selectById(request.getActualFounderId());
            if (actualFounder == null) {
                throw new BusinessException(404, "实际拾取者不存在");
            }
            item.setActualFounderId(request.getActualFounderId());
        }

        item.setPublishedAt(LocalDateTime.now());
        foundItemMapper.insert(item);
        try {
            imageFingerprintService.indexItemImages(item);
        } catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(FoundItemServiceImpl.class)
                    .warn("物品图片指纹建立失败: itemId={}", item.getId(), exception);
        }

        return FoundItemVO.fromEntity(item, founder.getRealName(), null,
                isNewUser(founder));
    }

    @Override
    public IPage<FoundItemVO> listPublic(ItemQueryRequest request) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode());

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(FoundItem::getTitle, request.getKeyword())
                    .or().like(FoundItem::getDescription, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(FoundItem::getCategory, request.getCategory());
        }
        if (request.getDropPointId() != null) {
            wrapper.eq(FoundItem::getDropPointId, request.getDropPointId());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    @Override
    public FoundItemVO getById(Long id) {
        FoundItem item = getItemOrThrow(id);
        return convertWithNames(List.of(item)).get(0);
    }

    @Override
    @Transactional
    public void handIn(Long id, Long dropPointId, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以交物");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode())) {
            throw new BusinessException(409, "物品当前状态不可交物");
        }

        DropPoint dropPoint = dropPointMapper.selectById(dropPointId);
        if (dropPoint == null || dropPoint.getStatus() != 1) {
            throw new BusinessException(404, "站点不存在或已停用");
        }

        item.setItemStatus(ItemStatus.PUBLIC.getCode());
        item.setDropPointId(dropPointId);
        foundItemMapper.updateById(item);

        HandInLog log = new HandInLog();
        log.setItemId(id);
        log.setDropPointId(dropPointId);
        log.setHandInStatus(1);
        log.setHandedInAt(LocalDateTime.now());
        handInLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void invalidate(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);

        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以作废");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode())
                && !item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())) {
            throw new BusinessException(409, "物品当前状态不可作废");
        }

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);

        LambdaQueryWrapper<HandInLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandInLog::getItemId, id);
        HandInLog log = handInLogMapper.selectOne(wrapper);
        if (log != null) {
            log.setHandInStatus(3);
            handInLogMapper.updateById(log);
        }
    }

    @Override
    @Transactional
    public void archive(Long id, Long currentUserId) {
        FoundItem item = getItemOrThrow(id);
        int status = item.getItemStatus();

        if (status == ItemStatus.PICKED_UP.getCode()) {
            // 3→4：发布者手动归档已取件物品
            if (!item.getFounderId().equals(currentUserId)) {
                throw new BusinessException(403, "只有发布者可以归档");
            }
        } else if (status == ItemStatus.EXPIRED.getCode()) {
            // 5→4：超期物品经线下处置后由管理员归档
            User operator = userMapper.selectById(currentUserId);
            if (operator == null || operator.getRole() != com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                throw new BusinessException(403, "只有系统管理员可以归档超期物品");
            }
        } else {
            throw new BusinessException(409, "物品当前状态不可归档");
        }

        item.setItemStatus(ItemStatus.ARCHIVED.getCode());
        foundItemMapper.updateById(item);
    }

    @Override
    public IPage<FoundItemVO> listMy(Long currentUserId, ItemQueryRequest request) {
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getFounderId, currentUserId);

        if (request.getItemStatus() != null) {
            wrapper.eq(FoundItem::getItemStatus, request.getItemStatus());
        }

        wrapper.orderByDesc(FoundItem::getPublishedAt);

        IPage<FoundItem> page = foundItemMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);

        return convertPage(page);
    }

    private void checkWeeklyLimit(Long userId) {        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getFounderId, userId)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode(),
                        ItemStatus.PUBLIC.getCode(),
                        ItemStatus.CLAIMING.getCode(),
                        ItemStatus.PICKED_UP.getCode())
                .ge(FoundItem::getPublishedAt, weekAgo);
        if (foundItemMapper.selectCount(wrapper) >= MAX_WEEKLY_PUBLISH) {
            throw new BusinessException(429, "每周最多发布3条有效招领，请下周再试");
        }
    }

    private IPage<FoundItemVO> convertPage(IPage<FoundItem> page) {
        List<FoundItemVO> voList = convertWithNames(page.getRecords());
        Page<FoundItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<FoundItemVO> convertWithNames(List<FoundItem> items) {
        List<Long> founderIds = items.stream()
                .map(FoundItem::getFounderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> dropPointIds = items.stream()
                .map(FoundItem::getDropPointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<User> founders = founderIds.isEmpty() ? List.of()
                : userMapper.selectBatchIds(founderIds);

        Map<Long, String> founderNames = founders.stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        Map<Long, Boolean> newUserMap = founders.stream()
                .collect(Collectors.toMap(User::getId, this::isNewUser));

        Map<Long, String> dropPointNames = dropPointIds.isEmpty() ? Map.of()
                : dropPointMapper.selectBatchIds(dropPointIds).stream()
                        .collect(Collectors.toMap(DropPoint::getId, DropPoint::getName));

        return items.stream()
                .map(item -> FoundItemVO.fromEntity(
                        item,
                        founderNames.get(item.getFounderId()),
                        dropPointNames.get(item.getDropPointId()),
                        newUserMap.getOrDefault(item.getFounderId(), false)))
                .collect(Collectors.toList());
    }

    private boolean isNewUser(User founder) {
        return founder.getCreatedAt() != null
                && founder.getCreatedAt().plusDays(NEW_USER_DAYS).isAfter(LocalDateTime.now());
    }

    private FoundItem getItemOrThrow(Long id) {
        FoundItem item = foundItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        return item;
    }
}
