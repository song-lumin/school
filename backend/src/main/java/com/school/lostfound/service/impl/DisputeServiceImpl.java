package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.DisputeService;
import com.school.lostfound.vo.DisputeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    /** 纠纷申诉类型 */
    public static final String TYPE_ITEM_MISMATCH = "ITEM_MISMATCH";
    public static final String TYPE_OTHER = "OTHER";
    /** 物品被冒领（失主发起，无时间窗口） */
    public static final String TYPE_FALSE_CLAIM = "FALSE_CLAIM";

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private static final Duration DISPUTE_SUBMIT_WINDOW = Duration.ofHours(24);

    private final DisputeMapper disputeMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final FoundItemMapper foundItemMapper;
    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    @Override
    @Transactional
    public DisputeVO createDispute(DisputeCreateRequest request, Long currentUserId) {
        String type = request.getDisputeType();
        if (!TYPE_ITEM_MISMATCH.equals(type) && !TYPE_OTHER.equals(type) && !TYPE_FALSE_CLAIM.equals(type)) {
            throw new BusinessException(400, "无效的纠纷类型");
        }

        if (TYPE_FALSE_CLAIM.equals(type)) {
            return createFalseClaimDispute(request, currentUserId);
        }

        if (request.getApplyId() == null) {
            throw new BusinessException(400, "认领申请ID不能为空");
        }

        ClaimApply apply = claimApplyMapper.selectById(request.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "认领申请不存在");
        }
        if (!apply.getClaimerId().equals(currentUserId)) {
            throw new BusinessException(403, "只有认领人本人可以提交纠纷申诉");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.COMPLETED.getCode())) {
            throw new BusinessException(409, "该申请尚未完成领取，无法申诉");
        }
        if (apply.getPickupTime() == null
                || apply.getPickupTime().isBefore(LocalDateTime.now().minus(DISPUTE_SUBMIT_WINDOW))) {
            throw new BusinessException(409, "申诉窗口已关闭（仅限领取后24小时内提交）");
        }

        FoundItem item = foundItemMapper.selectById(apply.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        Dispute dispute = new Dispute();
        dispute.setApplicantId(currentUserId);
        dispute.setApplyId(apply.getId());
        dispute.setItemId(item.getId());
        dispute.setDisputeType(type);
        dispute.setDescription(request.getDescription());
        dispute.setEvidenceImages(request.getEvidenceImages());
        dispute.setStatus(STATUS_PENDING);
        disputeMapper.insert(dispute);

        log.info("纠纷申诉已提交：disputeId={}, applyId={}, applicant={}", dispute.getId(), apply.getId(), currentUserId);
        return toVO(dispute, item.getTitle());
    }

    /**
     * 物品被冒领：任何人可声称自己是失主并发起，自动关联该物品最近一笔已完成认领订单，
     * 真伪由管理员审核裁定，无时间窗口。
     */
    private DisputeVO createFalseClaimDispute(DisputeCreateRequest request, Long currentUserId) {
        if (request.getItemId() == null) {
            throw new BusinessException(400, "物品ID不能为空");
        }

        FoundItem item = foundItemMapper.selectById(request.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        ClaimApply apply = claimApplyMapper.selectList(new LambdaQueryWrapper<ClaimApply>()
                        .eq(ClaimApply::getItemId, item.getId())
                        .eq(ClaimApply::getApplyStatus, ClaimStatus.COMPLETED.getCode())
                        .orderByDesc(ClaimApply::getPickupTime)
                        .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(409, "该物品没有已完成的认领订单，无法发起冒领申诉"));

        Dispute dispute = new Dispute();
        dispute.setApplicantId(currentUserId);
        dispute.setApplyId(apply.getId());
        dispute.setItemId(item.getId());
        dispute.setDisputeType(TYPE_FALSE_CLAIM);
        dispute.setDescription(request.getDescription());
        dispute.setEvidenceImages(request.getEvidenceImages());
        dispute.setStatus(STATUS_PENDING);
        disputeMapper.insert(dispute);

        log.info("冒领申诉已提交：disputeId={}, itemId={}, applyId={}, applicant={}",
                dispute.getId(), item.getId(), apply.getId(), currentUserId);
        return toVO(dispute, item.getTitle());
    }

    @Override
    public IPage<DisputeVO> list(String disputeType, Integer status, int page, int size) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(disputeType)) {
            wrapper.eq(Dispute::getDisputeType, disputeType);
        }
        if (status != null) {
            wrapper.eq(Dispute::getStatus, status);
        }
        wrapper.orderByAsc(Dispute::getStatus).orderByDesc(Dispute::getCreatedAt);

        IPage<Dispute> result = disputeMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> applicantIds = result.getRecords().stream()
                .map(Dispute::getApplicantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> applicantNames = applicantIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(applicantIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        List<Long> itemIds = result.getRecords().stream()
                .map(Dispute::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> itemTitles = itemIds.isEmpty() ? Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, FoundItem::getTitle));

        Page<DisputeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(d -> {
                    DisputeVO vo = toVO(d, itemTitles.get(d.getItemId()));
                    vo.setApplicantName(applicantNames.get(d.getApplicantId()));
                    return vo;
                })
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public DisputeVO getById(Long id, Long currentUserId) {
        Dispute dispute = getDisputeOrThrow(id);

        User user = userMapper.selectById(currentUserId);
        boolean isAdmin = user != null && user.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN;
        boolean isApplicant = dispute.getApplicantId().equals(currentUserId);
        if (!isAdmin && !isApplicant) {
            throw new BusinessException(403, "无权查看该申诉工单");
        }

        String itemTitle = null;
        if (dispute.getItemId() != null) {
            FoundItem item = foundItemMapper.selectById(dispute.getItemId());
            itemTitle = item != null ? item.getTitle() : null;
        }
        return toVO(dispute, itemTitle);
    }

    @Override
    public IPage<DisputeVO> listMy(Long currentUserId, int page, int size) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dispute::getApplicantId, currentUserId)
                .orderByDesc(Dispute::getCreatedAt);

        IPage<Dispute> result = disputeMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> itemIds = result.getRecords().stream()
                .map(Dispute::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> itemTitles = itemIds.isEmpty() ? Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, FoundItem::getTitle));

        Page<DisputeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(d -> toVO(d, itemTitles.get(d.getItemId())))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public void handle(Long id, DisputeHandleRequest request, Long operatorId) {
        Dispute dispute = getDisputeOrThrow(id);
        if (!dispute.getStatus().equals(STATUS_PENDING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }

        if (request.getApproved()) {
            dispute.setStatus(STATUS_APPROVED);
            if (TYPE_FALSE_CLAIM.equals(dispute.getDisputeType())) {
                handleFalseClaimApproval(dispute, request, operatorId);
            } else {
                handleDisputeApproval(dispute, request, operatorId);
            }
        } else {
            dispute.setStatus(STATUS_REJECTED);
        }

        dispute.setHandlerNote(request.getHandlerNote());
        dispute.setHandlerId(operatorId);
        dispute.setHandledAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);
    }

    /** 冒领申诉通过：回滚发布人+3、认领人+1，物品重新公开待认领，真正失主走正常流程认领 */
    private void handleFalseClaimApproval(Dispute dispute, DisputeHandleRequest request, Long operatorId) {
        if (dispute.getApplyId() == null) {
            throw new BusinessException(400, "冒领工单缺少关联认领申请，无法回滚积分");
        }
        ClaimApply apply = claimApplyMapper.selectById(dispute.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "关联认领申请不存在");
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null) {
            throw new BusinessException(404, "关联物品不存在");
        }

        String reason = "冒领申诉通过（工单#" + dispute.getId() + "）：" + request.getHandlerNote();

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);

        apply.setCreditRollback(1);
        claimApplyMapper.updateById(apply);

        item.setItemStatus(ItemStatus.PUBLIC.getCode());
        foundItemMapper.updateById(item);
    }

    /** 纠纷申诉通过：回滚发布人+3、认领人+1，物品作废 */
    private void handleDisputeApproval(Dispute dispute, DisputeHandleRequest request, Long operatorId) {
        if (dispute.getApplyId() == null) {
            throw new BusinessException(400, "纠纷工单缺少关联认领申请，无法回滚积分");
        }
        ClaimApply apply = claimApplyMapper.selectById(dispute.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "关联认领申请不存在");
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null) {
            throw new BusinessException(404, "关联物品不存在");
        }

        String reason = "纠纷申诉通过（工单#" + dispute.getId() + "）：" + request.getHandlerNote();

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);

        apply.setCreditRollback(1);
        claimApplyMapper.updateById(apply);

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);
    }

    private void rollbackSingle(Long userId, String operationType, int amount,
                                Long itemId, Long applyId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType)
                .eq(CreditLog::getRelatedItemId, itemId);
        if (creditLogMapper.selectCount(issuedWrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -amount);
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(userId, -amount, "ROLLBACK", itemId, applyId, reason, operatorId);
    }

    private Dispute getDisputeOrThrow(Long id) {
        Dispute dispute = disputeMapper.selectById(id);
        if (dispute == null) {
            throw new BusinessException(404, "申诉工单不存在");
        }
        return dispute;
    }

    private DisputeVO toVO(Dispute d, String itemTitle) {
        return DisputeVO.fromEntity(d, null, itemTitle);
    }
}
