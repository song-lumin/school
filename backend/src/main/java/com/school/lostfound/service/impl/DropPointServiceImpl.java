package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.lostfound.dto.CheckRequest;
import com.school.lostfound.dto.DropPointRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.HandInLog;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.HandInLogMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.DropPointService;
import com.school.lostfound.vo.ClaimApplyVO;
import com.school.lostfound.vo.DropPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DropPointServiceImpl implements DropPointService {

    private static final int CHECK_ISSUE_CREDIT = 1;

    private final DropPointMapper dropPointMapper;
    private final UserMapper userMapper;
    private final FoundItemMapper foundItemMapper;
    private final HandInLogMapper handInLogMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final CreditService creditService;

    @Override
    public List<DropPointVO> listAll() {
        List<DropPoint> dropPoints = dropPointMapper.selectList(
                new LambdaQueryWrapper<DropPoint>().orderByAsc(DropPoint::getId));

        List<Long> adminIds = dropPoints.stream()
                .map(DropPoint::getAdminId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> adminNames = adminIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        return dropPoints.stream()
                .map(dp -> DropPointVO.fromEntity(dp, adminNames.get(dp.getAdminId())))
                .collect(Collectors.toList());
    }

    @Override
    public DropPointVO getById(Long id) {
        DropPoint dropPoint = getDropPointOrThrow(id);
        String adminName = null;
        if (dropPoint.getAdminId() != null) {
            User admin = userMapper.selectById(dropPoint.getAdminId());
            if (admin != null) {
                adminName = admin.getRealName();
            }
        }
        return DropPointVO.fromEntity(dropPoint, adminName);
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
                        ItemStatus.CLAIMING.getCode());
        if (foundItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "站点下还有未处理完的物品，无法删除");
        }

        dropPoint.setStatus(0);
        dropPointMapper.updateById(dropPoint);
    }

    @Override
    public List<FoundItem> getPendingItems(Long dropPointId) {
        getDropPointOrThrow(dropPointId);

        LambdaQueryWrapper<FoundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoundItem::getDropPointId, dropPointId)
                .in(FoundItem::getItemStatus,
                        ItemStatus.PUBLISHED_NOT_HANDED_IN.getCode(),
                        ItemStatus.PUBLIC.getCode())
                .apply("id NOT IN (SELECT item_id FROM hand_in_log WHERE drop_point_id = {0} AND hand_in_status = 2)", dropPointId)
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
                        .collect(Collectors.toMap(User::getId, User::getRealName));

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
