package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.DropPointService;
import com.school.lostfound.service.MessageService;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.DropPointVO;
import com.school.lostfound.vo.InventoryItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DropPointServiceImpl implements DropPointService {

    private static final int CHECK_ISSUE_CREDIT = 1;
    /** 「物品损坏」纠纷类型：损坏件回收后必须作废，绝不重回公开待认领池 */
    private static final String DISPUTE_TYPE_ITEM_DAMAGED = "ITEM_DAMAGED";
    private static final int DISPUTE_STATUS_APPROVED = 1;
    private static final int DISPUTE_STATUS_CREDIT_PENDING = 4;

    private final DropPointMapper dropPointMapper;
    private final UserMapper userMapper;
    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final DisputeMapper disputeMapper;
    private final CreditService creditService;
    private final MessageService messageService;

    @Override
    public List<DropPointVO> listAll() {
        List<DropPoint> dropPoints = dropPointMapper.selectList(
                new LambdaQueryWrapper<DropPoint>().orderByAsc(DropPoint::getId));

        List<Long> adminIds = dropPoints.stream()
                .map(DropPoint::getAdminId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> adminUsers = adminIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return dropPoints.stream()
                .map(dp -> {
                    User admin = adminUsers.get(dp.getAdminId());
                    return DropPointVO.fromEntity(dp,
                            admin == null ? null : admin.getRealName(),
                            admin == null ? null : admin.getPhone(),
                            admin == null ? null : admin.getEmail());
                })
                .collect(Collectors.toList());
    }

    @Override
    public DropPointVO getById(Long id) {
        DropPoint dropPoint = getDropPointOrThrow(id);
        User admin = null;
        if (dropPoint.getAdminId() != null) {
            admin = userMapper.selectById(dropPoint.getAdminId());
        }
        return DropPointVO.fromEntity(dropPoint,
                admin == null ? null : admin.getRealName(),
                admin == null ? null : admin.getPhone(),
                admin == null ? null : admin.getEmail());
    }

    @Override
    @Transactional
    public DropPointVO create(DropPointRequest request) {
        validateAdmin(request.getAdminId());

        DropPoint dropPoint = new DropPoint();
        copyRequest(request, dropPoint);
        dropPoint.setStatus(1);
        dropPointMapper.insert(dropPoint);

        return DropPointVO.fromEntity(dropPoint, null);
    }

    @Override
    @Transactional
    public DropPointVO update(Long id, DropPointRequest request) {
        DropPoint dropPoint = getDropPointOrThrow(id);
        validateAdmin(request.getAdminId());

        copyRequest(request, dropPoint);
        dropPointMapper.updateById(dropPoint);

        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DropPoint dropPoint = getDropPointOrThrow(id);

        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getDropPointId, id)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLIC.getCode(),
                        ItemStatus.CLAIMING.getCode(),
                        // 站内还有实物或有待线下动作的件也不能停站：否则没人认领/确认归还/处置，流程悬空
                        ItemStatus.PICKED_UP.getCode(),
                        ItemStatus.PENDING_PLACEMENT.getCode(),
                        ItemStatus.RETURNING.getCode(),
                        ItemStatus.LOST.getCode(),
                        ItemStatus.EXPIRED.getCode());
        if (foundItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409,
                    "站点下还有未闭环的物品（待投放/认领中/已取件/待归还/丢失待查/超期），请先处理完再停用");
        }

        dropPoint.setStatus(0);
        dropPointMapper.updateById(dropPoint);
    }

    @Override
    public List<FoundItem> getPendingItems(Long dropPointId) {
        getDropPointOrThrow(dropPointId);

        // 巡检清单包含两类：
        // 1. 公开待认领(1)但hand_in_status≠2：正常待巡检
        // 2. 已取件(3)但hand_in_status=1：失主已在巡检前领取，需核对凭证后补发1分
        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getDropPointId, dropPointId)
                .and(w -> w
                        .nested(n -> n.eq(FoundItem::getItemStatus, ItemStatus.PUBLIC.getCode())
                                .apply("id NOT IN (SELECT item_id FROM hand_in_log WHERE drop_point_id = {0} AND hand_in_status = 2)", dropPointId))
                        .or(n -> n.eq(FoundItem::getItemStatus, ItemStatus.PICKED_UP.getCode())
                                .apply("id IN (SELECT item_id FROM hand_in_log WHERE drop_point_id = {0} AND hand_in_status = 1)", dropPointId))
                )
                .orderByDesc(FoundItem::getPublishedAt);
        return foundItemMapper.selectList(wrapper);
    }

    @Override
    public List<ClaimApplyVO> getPendingPickups(Long dropPointId) {
        getDropPointOrThrow(dropPointId);

        LambdaQueryWrapper<FoundItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FoundItem::getDropPointId, dropPointId)
                .eq(FoundItem::getItemStatus, ItemStatus.CLAIMING.getCode());
        List<FoundItem> claimingItems = foundItemMapper.selectList(itemWrapper);
        if (claimingItems.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = claimingItems.stream().map(FoundItem::getId).toList();
        LambdaQueryWrapper<ClaimApply> applyWrapper = new LambdaQueryWrapper<>();
        applyWrapper.in(ClaimApply::getItemId, itemIds)
                .eq(ClaimApply::getApplyStatus, ClaimStatus.APPROVED_WAITING_PICKUP.getCode())
                .orderByDesc(ClaimApply::getCreatedAt);
        List<ClaimApply> applies = claimApplyMapper.selectList(applyWrapper);

        Map<Long, String> itemTitles = claimingItems.stream()
                .collect(Collectors.toMap(FoundItem::getId, FoundItem::getTitle));
        List<Long> claimerIds = applies.stream()
                .map(ClaimApply::getClaimerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> claimerNames = claimerIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(claimerIds).stream()
                        .collect(Collectors.toMap(User::getId,
                                u -> u.getRealName() == null ? "" : u.getRealName(), (a, b) -> a));

        return applies.stream()
                .map(apply -> ClaimApplyVO.fromEntity(
                        apply,
                        itemTitles.get(apply.getItemId()),
                        null,
                        claimerNames.get(apply.getClaimerId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void checkItem(Long dropPointId, CheckRequest request, Long operatorId) {
        DropPoint dropPoint = getDropPointOrThrow(dropPointId);

        FoundItem item = foundItemMapper.selectById(request.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!dropPointId.equals(item.getDropPointId())) {
            throw new BusinessException(400, "物品不属于该站点");
        }

        LambdaQueryWrapper<HandInLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(HandInLog::getItemId, item.getId());
        HandInLog handInLog = handInLogMapper.selectOne(logWrapper);

        if (handInLog == null || !handInLog.getHandInStatus().equals(1)) {
            throw new BusinessException(409, "物品未交物或已巡检");
        }

        handInLog.setHandInStatus(2);
        handInLog.setCheckedAt(LocalDateTime.now());
        handInLog.setCheckedBy(operatorId);
        handInLog.setCheckNote(request.getCheckNote());
        handInLog.setCreditIssued(1);
        handInLog.setCreditIssuedAt(LocalDateTime.now());
        handInLogMapper.updateById(handInLog);

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        creditService.issueCredit(publisherId, CHECK_ISSUE_CREDIT, "CHECK_ISSUE",
                item.getId(), null, "物品巡检通过", operatorId);
    }
    @Override
    @Transactional
    public String returnIn(Long dropPointId, Long itemId, Long operatorId) {
        DropPoint dropPoint = getDropPointOrThrow(dropPointId);

        FoundItem item = foundItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!dropPointId.equals(item.getDropPointId())) {
            throw new BusinessException(400, "物品不属于该站点");
        }
        if (!item.getItemStatus().equals(ItemStatus.RETURNING.getCode())) {
            throw new BusinessException(409, "只有「待归还」的物品可以确认入库");
        }

        // 优先读定责时落库的处置结论：站点给结论、系统执行后写进 site_dispose，
        // 不再靠「有没有 ITEM_DAMAGED 工单」反查，避免站点处置与工单类型两套口径打架
        Dispute latestVerdict = disputeMapper.selectOne(new LambdaQueryWrapper<Dispute>()
                .eq(Dispute::getItemId, itemId)
                .in(Dispute::getStatus, DISPUTE_STATUS_APPROVED, DISPUTE_STATUS_CREDIT_PENDING)
                .orderByDesc(Dispute::getId)
                .last("LIMIT 1"));
        String disposed = latestVerdict == null ? null : latestVerdict.getSiteDispose();
        // 损坏案的结论去向写的是「待归还」（等站点把实物收回来），但入库这一秒必须作废：
        // 只看 site_dispose 会把 RETURNING 当成「重新公开」，把坏件又扔回公开池
        boolean damagedDispute = latestVerdict != null
                && DISPUTE_TYPE_ITEM_DAMAGED.equals(latestVerdict.getDisputeType());
        boolean damagedVerdict = damagedDispute
                || "VOIDED".equalsIgnoreCase(disposed)
                || (disposed == null && disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>()
                .eq(Dispute::getItemId, itemId)
                .eq(Dispute::getDisputeType, DISPUTE_TYPE_ITEM_DAMAGED)
                .eq(Dispute::getStatus, DISPUTE_STATUS_APPROVED)) > 0);

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FoundItem> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(FoundItem::getId, itemId)
                .set(FoundItem::getItemStatus,
                        damagedVerdict ? ItemStatus.VOIDED.getCode() : ItemStatus.PUBLIC.getCode());
        if (damagedVerdict) {
            updateWrapper.set(FoundItem::getTakedownReason, "纠纷定责：物品损坏，作废不再重新公开")
                    .set(FoundItem::getTakedownAt, LocalDateTime.now())
                    .set(FoundItem::getTakedownBy, operatorId);
        } else {
            updateWrapper.set(FoundItem::getClaimedAt, null)
                    // 重新公开即重新计时：不重置的话，发布已超 7 天的易腐件放回后会被当晚的超期任务直接归档
                    .set(FoundItem::getPublishedAt, LocalDateTime.now())
                    .set(FoundItem::getExpireWarningSent, 0);
        }
        foundItemMapper.update(null, updateWrapper);

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();

        ClaimApply lastApply = claimApplyMapper.selectOne(new LambdaQueryWrapper<ClaimApply>()
                .eq(ClaimApply::getItemId, itemId)
                .eq(ClaimApply::getCreditRollback, 1)
                .orderByDesc(ClaimApply::getId)
                .last("LIMIT 1"));

        if (damagedVerdict) {
            messageService.push(publisherId, "损坏件已作废 · 不再重新公开",
                    "你的招领物品「" + item.getTitle() + "」经「物品损坏」定责后，已由「" + dropPoint.getName()
                            + "」确认收回并作废归档。损坏件不会再进入公开待认领池，也不会再次对外开放认领；"
                            + "物品归属仍为你，如另有完好副本可重新发布。",
                    "RETURN", item.getId(), null);
            if (lastApply != null) {
                messageService.push(lastApply.getClaimerId(), "损坏物品已收回 · 作废归档",
                        "物品「" + item.getTitle() + "」已由「" + dropPoint.getName()
                                + "」确认收回。因该件已定责为「物品损坏」，不会再重新公开待认领，本次流程到此结束。感谢你把物品送回站点。",
                        "RETURN", item.getId(), lastApply.getId());
            }
            log.info("损坏件入库作废：itemId={}, dropPointId={}, operator={}", itemId, dropPointId, operatorId);
            return "VOIDED";
        }

        // 归还入库后通知双方：物品归属不变，已重新公开待认领
        messageService.push(publisherId, "物品已归还入库 · 重新公开待认领",
                "你的招领物品「" + item.getTitle() + "」已被误领人送回「" + dropPoint.getName()
                        + "」并由站点确认入库，现重新公开待认领。此前被判无效的成交若已回滚拾获奖励，真失主再次认领取件时会重新结算；下一位认领人仍会正常获得认领奖励。物品归属仍为你。",
                "RETURN", item.getId(), null);

        if (lastApply != null) {
            messageService.push(lastApply.getClaimerId(), "物品已收回 · 感谢归还",
                    "物品「" + item.getTitle() + "」已由「" + dropPoint.getName()
                            + "」确认收回入库，并重新公开待认领。感谢你把物品送回站点。",
                    "RETURN", item.getId(), lastApply.getId());
        }
        return "PUBLIC";
    }

    @Override
    public List<InventoryItemVO> getInventory(Long dropPointId) {
        getDropPointOrThrow(dropPointId);

        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getDropPointId, dropPointId)
                .orderByDesc(FoundItem::getPublishedAt);
        List<FoundItem> items = foundItemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(FoundItem::getId).toList();

        LambdaQueryWrapper<HandInLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.in(HandInLog::getItemId, itemIds);
        Map<Long, HandInLog> logMap = handInLogMapper.selectList(logWrapper).stream()
                .collect(Collectors.toMap(HandInLog::getItemId, l -> l, (a, b) -> a));

        List<Long> founderIds = items.stream()
                .map(i -> i.getActualFounderId() != null ? i.getActualFounderId() : i.getFounderId())
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> founderNames = founderIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(founderIds).stream()
                        .collect(Collectors.toMap(User::getId,
                                u -> u.getRealName() == null ? "" : u.getRealName(), (a, b) -> a));

        LambdaQueryWrapper<ClaimApply> claimWrapper = new LambdaQueryWrapper<>();
        claimWrapper.in(ClaimApply::getItemId, itemIds)
                .eq(ClaimApply::getApplyStatus, 1);
        Map<Long, ClaimApply> successClaimMap = claimApplyMapper.selectList(claimWrapper).stream()
                .collect(Collectors.toMap(ClaimApply::getItemId, c -> c, (a, b) -> a));
        Map<Long, String> claimerMap = new java.util.HashMap<>();
        successClaimMap.forEach((itemId, c) -> {
            if (c.getClaimerId() != null) {
                User u = userMapper.selectById(c.getClaimerId());
                claimerMap.put(itemId, u != null ? u.getRealName() : null);
            }
        });

        return items.stream().map(i -> {
            InventoryItemVO vo = new InventoryItemVO();
            vo.setItem(i);
            HandInLog log = logMap.get(i.getId());
            if (log != null) {
                vo.setHandInStatus(log.getHandInStatus());
                vo.setHandInAt(log.getHandedInAt());
                vo.setCheckedAt(log.getCheckedAt());
                vo.setCheckNote(log.getCheckNote());
                vo.setCreditIssued(log.getCreditIssued());
            } else {
                vo.setHandInStatus(0);
            }
            Long fid = i.getActualFounderId() != null ? i.getActualFounderId() : i.getFounderId();
            vo.setFounderName(founderNames.get(fid));
            vo.setClaimerName(claimerMap.get(i.getId()));
            ClaimApply sc = successClaimMap.get(i.getId());
            if (sc != null) vo.setPickupTime(sc.getPickupTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private void validateAdmin(Long adminId) {
        if (adminId == null) {
            return;
        }
        User admin = userMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(404, "指定的管理员不存在");
        }
        if (admin.getRole() != com.school.lostfound.enums.UserRole.POINT_ADMIN
                && admin.getRole() != com.school.lostfound.enums.UserRole.SYS_ADMIN) {
            throw new BusinessException(400, "指定的用户不是站点管理员");
        }
    }

    private void copyRequest(DropPointRequest request, DropPoint dropPoint) {
        dropPoint.setName(request.getName());
        dropPoint.setLocation(request.getLocation());
        dropPoint.setAdminId(request.getAdminId());
        dropPoint.setHasCamera(request.getHasCamera() != null ? request.getHasCamera() : 0);
        dropPoint.setCameraInfo(request.getCameraInfo());
    }

    private DropPoint getDropPointOrThrow(Long id) {
        DropPoint dropPoint = dropPointMapper.selectById(id);
        if (dropPoint == null) {
            throw new BusinessException(404, "站点不存在");
        }
        return dropPoint;
    }
}
