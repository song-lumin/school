package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.ClaimService;
import com.school.lostfound.service.ClaimConfidenceAnalyzer;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.RiskControlService;
import com.school.lostfound.vo.ClaimApplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final int MAX_REJECT_COUNT = 3;
    private static final int LOCK_HOURS = 24;
    private static final int CHECK_ISSUE_CREDIT = 1;
    private static final int PICKUP_ISSUE_CREDIT = 3;
    private static final int CLAIMANT_CREDIT = 1;

    private final ClaimApplyMapper claimApplyMapper;
    private final FoundItemMapper foundItemMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    private final RiskControlService riskControlService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public ClaimApplyVO apply(ClaimApplyRequest request, Long currentUserId) {
        return applyInternal(request, null, currentUserId);
    }

    @Override
    @Transactional
    public ClaimApplyVO applyForward(ClaimApplyRequest request, Long sourceNoticeId, Long currentUserId) {
        return applyInternal(request, sourceNoticeId, currentUserId);
    }

    private ClaimApplyVO applyInternal(ClaimApplyRequest request, Long sourceNoticeId, Long currentUserId) {
        if (riskControlService.containsSensitiveWord(request.getAnswer())) {
            throw new BusinessException(400, "回答内容包含敏感词，请修改后重新提交");
        }

        FoundItem item = foundItemMapper.selectById(request.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())) {
            throw new BusinessException(409, "物品当前不可认领");
        }
        if (item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(400, "不能认领自己发布的物品");
        }

        String lockKey = "claim:lock:" + request.getItemId() + ":" + currentUserId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(409, "请勿重复提交");
        }

        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApply::getItemId, request.getItemId())
                .eq(ClaimApply::getClaimerId, currentUserId);
        ClaimApply existing = claimApplyMapper.selectOne(wrapper);

        if (existing != null) {
            if (existing.getApplyStatus().equals(ClaimStatus.LOCKED.getCode())) {
                // 第3次驳回时updatedAt即锁定时间，满24h自动解锁放行
                if (existing.getUpdatedAt().plusHours(LOCK_HOURS).isBefore(LocalDateTime.now())) {
                    existing.setApplyStatus(ClaimStatus.REJECTED.getCode());
                    claimApplyMapper.updateById(existing);
                } else {
                    throw new BusinessException(409, "认领被驳回3次，已锁定，请24小时后再试");
                }
            }
            if (existing.getApplyStatus().equals(ClaimStatus.PENDING.getCode())
                    || existing.getApplyStatus().equals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode())) {
                throw new BusinessException(409, "您已提交过认领申请，请等待审核");
            }
        }

        ClaimApply apply = existing != null ? existing : new ClaimApply();
        ClaimConfidenceAnalyzer.Analysis confidence = ClaimConfidenceAnalyzer.analyze(
                request.getAnswer(), item.getTitle() + " " + item.getDescription());
        apply.setItemId(request.getItemId());
        apply.setSourceNoticeId(sourceNoticeId);
        apply.setClaimerId(currentUserId);
        apply.setAnswer(request.getAnswer());
        apply.setConfidenceScore(confidence.score());
        apply.setLowConfidence(confidence.lowConfidence() ? 1 : 0);
        apply.setConfidenceReason(confidence.reason());
        apply.setApplyStatus(ClaimStatus.PENDING.getCode());
        apply.setCreditIssued(0);
        apply.setCreditRollback(0);

        if (existing == null) {
            apply.setRejectCount(0);
            claimApplyMapper.insert(apply);
        } else {
            // 重新申请保留累计拒绝次数（累计3次锁定），不能清零
            claimApplyMapper.updateById(apply);
        }

        User claimer = userMapper.selectById(currentUserId);
        return ClaimApplyVO.fromEntity(apply, item.getTitle(), item.getClaimQuestion(),
                claimer != null ? claimer.getRealName() : null);
    }

    @Override
    public IPage<ClaimApplyVO> listByItem(Long itemId, Long currentUserId, int page, int size) {
        FoundItem item = foundItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以查看认领申请");
        }

        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApply::getItemId, itemId)
                .orderByDesc(ClaimApply::getCreatedAt);

        return convertPage(claimApplyMapper.selectPage(new Page<>(page, size), wrapper), item);
    }

    @Override
    public ClaimApplyVO getById(Long id, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        boolean isFounder = item != null && item.getFounderId().equals(currentUserId);
        boolean isClaimer = apply.getClaimerId().equals(currentUserId);
        if (!isFounder && !isClaimer) {
            throw new BusinessException(403, "无权查看该申请");
        }

        return convertSingle(apply, item);
    }

    @Override
    @Transactional
    public void approve(Long id, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以审核");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.PENDING.getCode())) {
            throw new BusinessException(409, "申请当前状态不可审核");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())) {
            throw new BusinessException(409, "物品当前状态不可审核认领");
        }

        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        claimApplyMapper.updateById(apply);

        // 同意一人后，同物品其余待审核申请全部驳回，不再挂着
        LambdaQueryWrapper<ClaimApply> othersWrapper = new LambdaQueryWrapper<>();
        othersWrapper.eq(ClaimApply::getItemId, item.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.PENDING.getCode())
                .ne(ClaimApply::getId, apply.getId());
        ClaimApply othersUpdate = new ClaimApply();
        othersUpdate.setApplyStatus(ClaimStatus.REJECTED.getCode());
        claimApplyMapper.update(othersUpdate, othersWrapper);

        item.setItemStatus(ItemStatus.CLAIMING.getCode());
        foundItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void reject(Long id, ClaimRejectRequest request, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!item.getFounderId().equals(currentUserId)) {
            throw new BusinessException(403, "只有发布者可以审核");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.PENDING.getCode())) {
            throw new BusinessException(409, "申请当前状态不可审核");
        }

        int newRejectCount = apply.getRejectCount() + 1;
        apply.setRejectReason(request.getRejectReason());
        apply.setRejectCount(newRejectCount);

        if (newRejectCount >= MAX_REJECT_COUNT) {
            apply.setApplyStatus(ClaimStatus.LOCKED.getCode());
        } else {
            apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
        }
        claimApplyMapper.updateById(apply);

        if (item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            LambdaQueryWrapper<ClaimApply> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.eq(ClaimApply::getItemId, item.getId())
                    .eq(ClaimApply::getApplyStatus, ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
            if (claimApplyMapper.selectCount(pendingWrapper) == 0) {
                item.setItemStatus(ItemStatus.PUBLIC.getCode());
                foundItemMapper.updateById(item);
            }
        }
    }

    @Override
    @Transactional
    public ClaimApplyVO pickup(Long id, String pickupPhoto, String pickupSignature, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        boolean isFounder = item.getFounderId().equals(currentUserId);
        boolean isPointAdmin = isUserPointAdmin(currentUserId);
        if (!isFounder && !isPointAdmin) {
            throw new BusinessException(403, "只有发布者或站点管理员可以确认取件");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode())) {
            throw new BusinessException(409, "申请当前状态不可取件");
        }
        if (pickupPhoto == null || pickupSignature == null) {
            throw new BusinessException(400, "取件照片和签名不能为空");
        }

        apply.setApplyStatus(ClaimStatus.COMPLETED.getCode());
        apply.setPickupTime(LocalDateTime.now());
        apply.setPickupPhoto(pickupPhoto);
        apply.setPickupSignature(pickupSignature);
        apply.setCreditIssued(1);
        claimApplyMapper.updateById(apply);

        item.setItemStatus(ItemStatus.PICKED_UP.getCode());
        item.setClaimedAt(LocalDateTime.now());
        foundItemMapper.updateById(item);

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        creditService.issueCredit(publisherId, PICKUP_ISSUE_CREDIT, "PICKUP_ISSUE",
                item.getId(), apply.getId(), "失主取件完成（拾取者）", currentUserId);
        creditService.issueCredit(apply.getClaimerId(), CLAIMANT_CREDIT, "PICKUP_ISSUE",
                item.getId(), apply.getId(), "失主取件完成（认领者）", currentUserId);

        return convertSingle(apply, item);
    }

    @Override
    public IPage<ClaimApplyVO> listMy(Long currentUserId, int page, int size) {
        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApply::getClaimerId, currentUserId)
                .orderByDesc(ClaimApply::getCreatedAt);

        return convertPage(claimApplyMapper.selectPage(new Page<>(page, size), wrapper), null);
    }

    private boolean isUserPointAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() == com.school.lostfound.enums.UserRole.POINT_ADMIN;
    }

    private IPage<ClaimApplyVO> convertPage(IPage<ClaimApply> page, FoundItem knownItem) {
        List<ClaimApplyVO> voList = convertList(page.getRecords(), knownItem);
        Page<ClaimApplyVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<ClaimApplyVO> convertList(List<ClaimApply> applies, FoundItem knownItem) {
        List<Long> itemIds = knownItem == null
                ? applies.stream().map(ClaimApply::getItemId).filter(Objects::nonNull).distinct().toList()
                : List.of();

        List<Long> claimerIds = applies.stream()
                .map(ClaimApply::getClaimerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, FoundItem> itemMap = knownItem != null
                ? Map.of(knownItem.getId(), knownItem)
                : (itemIds.isEmpty() ? Map.of()
                        : foundItemMapper.selectBatchIds(itemIds).stream()
                                .collect(Collectors.toMap(FoundItem::getId, i -> i)));

        Map<Long, String> claimerNames = claimerIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(claimerIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        return applies.stream()
                .map(apply -> {
                    FoundItem item = knownItem != null ? knownItem : itemMap.get(apply.getItemId());
                    return ClaimApplyVO.fromEntity(
                            apply,
                            item != null ? item.getTitle() : null,
                            item != null ? item.getClaimQuestion() : null,
                            claimerNames.get(apply.getClaimerId()));
                })
                .collect(Collectors.toList());
    }

    private ClaimApplyVO convertSingle(ClaimApply apply, FoundItem item) {
        User claimer = userMapper.selectById(apply.getClaimerId());
        return ClaimApplyVO.fromEntity(
                apply,
                item != null ? item.getTitle() : null,
                item != null ? item.getClaimQuestion() : null,
                claimer != null ? claimer.getRealName() : null);
    }

    private ClaimApply getApplyOrThrow(Long id) {
        ClaimApply apply = claimApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(404, "认领申请不存在");
        }
        return apply;
    }
}
