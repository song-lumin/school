package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.ClaimApplyRequest;
import com.school.lostfound.dto.ClaimRejectRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.ClaimService;
import com.school.lostfound.service.ClaimConfidenceAnalyzer;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.MessageService;
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
    private final DropPointMapper dropPointMapper;
    private final CreditService creditService;
    private final RiskControlService riskControlService;
    private final MessageService messageService;
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
                // 锁定24h到期后解锁，wrong_try_count清零，方可再次提交回答
                if (existing.getUpdatedAt() == null
                        || existing.getUpdatedAt().plusHours(LOCK_HOURS).isBefore(LocalDateTime.now())) {
                    existing.setApplyStatus(ClaimStatus.REJECTED.getCode());
                    existing.setRejectCount(0);
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

        // 查询用户信用分和历史成功认领次数，用于置信度综合计算
        User claimer = userMapper.selectById(currentUserId);
        int creditScore = claimer != null && claimer.getCreditScore() != null ? claimer.getCreditScore() : 0;
        LambdaQueryWrapper<ClaimApply> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.eq(ClaimApply::getClaimerId, currentUserId)
                .eq(ClaimApply::getApplyStatus, ClaimStatus.COMPLETED.getCode());
        long successCount = claimApplyMapper.selectCount(successWrapper);
        int existingRejectCount = existing != null && existing.getRejectCount() != null ? existing.getRejectCount() : 0;

        ClaimConfidenceAnalyzer.Analysis confidence = ClaimConfidenceAnalyzer.analyze(
                request.getAnswer(), item.getTitle() + " " + item.getDescription(),
                item.getReferenceAnswer(),
                creditScore, (int) successCount, existingRejectCount);
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
        // 重新提交后旧的驳回原因必须清掉，否则新申请还挂着上一轮的驳回理由
        apply.setRejectReason(null);

        if (existing == null) {
            apply.setRejectCount(0);
            claimApplyMapper.insert(apply);
        } else {
            claimApplyMapper.updateById(apply);
        }

        // 有新申请时通知发布人，否则招领只能靠自己反复刷列表才知道有人来认
        Long applyPublisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        String applyClaimerName = claimer == null ? "用户"
                : (claimer.getRealName() != null && !claimer.getRealName().isBlank()
                ? claimer.getRealName() : claimer.getUsername());
        String confidenceHint = confidence.lowConfidence()
                ? "该回答置信度偏低（" + confidence.score() + " 分），建议重点核对特征细节。"
                : "该回答置信度 " + confidence.score() + " 分。";
        messageService.push(applyPublisherId, "有新的认领申请 · 待你审核",
                applyClaimerName + " 申请认领你发布的「" + item.getTitle() + "」。" + confidenceHint
                        + "请到「认领审核」查看回答，决定通过或驳回。",
                "CLAIM", item.getId(), apply.getId());

        return ClaimApplyVO.fromEntity(apply, item.getTitle(), item.getClaimQuestion(),
                claimer != null ? claimer.getRealName() : null);
    }

    @Override
    public IPage<ClaimApplyVO> listByItem(Long itemId, Long currentUserId, int page, int size) {
        FoundItem item = foundItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        if (!canReviewClaim(item, currentUserId)) {
            throw new BusinessException(403, "只有发布者或管理员可以查看认领申请");
        }

        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApply::getItemId, itemId)
                .orderByDesc(ClaimApply::getConfidenceScore)
                .orderByDesc(ClaimApply::getCreatedAt);

        return convertPage(claimApplyMapper.selectPage(new Page<>(page, size), wrapper), item);
    }

    @Override
    public ClaimApplyVO getById(Long id, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        boolean isClaimer = apply.getClaimerId().equals(currentUserId);
        boolean canReview = item != null && canReviewClaim(item, currentUserId);
        if (!canReview && !isClaimer) {
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
        if (!canReviewClaim(item, currentUserId)) {
            throw new BusinessException(403, "只有发布者或管理员可以审核");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.PENDING.getCode())) {
            throw new BusinessException(409, "申请当前状态不可审核");
        }
        if (!item.getItemStatus().equals(ItemStatus.PUBLIC.getCode())) {
            throw new BusinessException(409, "物品当前状态不可审核认领");
        }

        apply.setApplyStatus(ClaimStatus.APPROVED_WAITING_PICKUP.getCode());
        claimApplyMapper.updateById(apply);

        // 通知申请人：可以去取件了，附站点位置与现场联系人，避免到站找不到人
        DropPoint point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        User pointAdmin = point == null || point.getAdminId() == null ? null : userMapper.selectById(point.getAdminId());
        StringBuilder content = new StringBuilder();
        content.append("你认领的「").append(item.getTitle()).append("」已审核通过，请到「")
                .append(point == null || point.getName() == null ? "投放点" : point.getName()).append("」取件。");
        if (point != null && point.getLocation() != null && !point.getLocation().isBlank()) {
            content.append("站点位置：").append(point.getLocation()).append("。");
        }
        if (pointAdmin != null) {
            content.append("现场联系：").append(pointAdmin.getRealName() != null && !pointAdmin.getRealName().isBlank()
                    ? pointAdmin.getRealName() : pointAdmin.getUsername());
            if (pointAdmin.getPhone() != null && !pointAdmin.getPhone().isBlank()) {
                content.append("（").append(pointAdmin.getPhone()).append("）");
            }
            content.append("。");
        }
        content.append("到场请带本人证件，与站点当面核对物品后再签字确认，确认取件后才会发放诚信分；")
                .append("若发现东西不对，直接在「我的认领」点「取消认领」即可，不扣分。");
        messageService.push(apply.getClaimerId(), "认领已通过 · 请到站点取件", content.toString(),
                "CLAIM", item.getId(), apply.getId());

        // 同意一人后，同物品其余待审核申请全部锁定(3)，不再允许新申请，但保留记录待解锁
        LambdaQueryWrapper<ClaimApply> othersWrapper = new LambdaQueryWrapper<>();
        othersWrapper.eq(ClaimApply::getItemId, item.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.PENDING.getCode())
                .ne(ClaimApply::getId, apply.getId());
        List<ClaimApply> othersLocked = claimApplyMapper.selectList(othersWrapper);
        if (!othersLocked.isEmpty()) {
            ClaimApply othersUpdate = new ClaimApply();
            othersUpdate.setApplyStatus(ClaimStatus.LOCKED.getCode());
            claimApplyMapper.update(othersUpdate, othersWrapper);
            // 被暂停的申请人不告知就会一直干等；对方现场退回时这条申请会一并作废，需要重新提交
            for (ClaimApply other : othersLocked) {
                messageService.push(other.getClaimerId(), "你的认领申请已暂停",
                        "物品「" + item.getTitle() + "」已由他人认领审核通过，你的申请暂时无法继续。"
                                + "若对方到场后取消认领，物品会重新回到「公开待认领」，但本次排队申请将一并作废，"
                                + "届时需要你重新提交认领。",
                        "CLAIM", item.getId(), other.getId());
            }
        }

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
        if (!canReviewClaim(item, currentUserId)) {
            throw new BusinessException(403, "只有发布者或管理员可以审核");
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

        // 驳回必须带原因，否则用户只能反复试错；顺带说清还剩几次机会 / 是否已被锁定
        String rejectReason = request.getRejectReason() == null || request.getRejectReason().isBlank()
                ? "回答与物品特征不符" : request.getRejectReason();
        String remainHint = newRejectCount >= MAX_REJECT_COUNT
                ? "已累计驳回 " + newRejectCount + " 次，该物品的认领资格锁定 24 小时，期间无法再次提交。"
                : "还可再提交 " + (MAX_REJECT_COUNT - newRejectCount) + " 次，请补充更具体的特征描述后重试。";
        messageService.push(apply.getClaimerId(), "认领申请被驳回",
                "你对「" + item.getTitle() + "」的认领申请未通过。驳回原因：" + rejectReason + "。" + remainHint,
                "CLAIM", item.getId(), apply.getId());

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
        // 领取确认由失主本人操作（现场找到实物后签字合影）
        if (!apply.getClaimerId().equals(currentUserId)) {
            throw new BusinessException(403, "只有认领人本人可以确认取件");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode())) {
            throw new BusinessException(409, "申请当前状态不可取件");
        }
        // 物品必须仍处于「认领中」才允许交付：丢失待查(8)、待归还(7)、已作废(0)、归档(4) 不能被一次取件覆盖
        if (!item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            throw new BusinessException(409, "物品当前状态不可交付（仅「认领中」的物品可以确认取件）");
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

        // 此前被锁定的其余认领回复统一标记为2认领驳回
        LambdaQueryWrapper<ClaimApply> lockedWrapper = new LambdaQueryWrapper<>();
        lockedWrapper.eq(ClaimApply::getItemId, item.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode());
        ClaimApply lockedUpdate = new ClaimApply();
        lockedUpdate.setApplyStatus(ClaimStatus.REJECTED.getCode());
        claimApplyMapper.update(lockedUpdate, lockedWrapper);

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        // 发布人拾获奖励按「成交」结算，但同一件物品同时只允许存在一笔未回滚的 +3：
        // 首次成交正常发放；上一次成交被判无效（领错 / 冒领 / 其他）并已回滚后，
        // 真失主再次认领属于新的一次有效成交，重新结算 +3；未被回滚则不再重复发放。
        if (!creditService.hasLivePickupReward(publisherId, item.getId())) {
            creditService.issueCreditOnce(publisherId, PICKUP_ISSUE_CREDIT, "PICKUP_ISSUE",
                    item.getId(), apply.getId(), "失主取件完成（拾获者）", currentUserId);
        }
        creditService.issueCreditOnce(apply.getClaimerId(), CLAIMANT_CREDIT, "PICKUP_ISSUE",
                item.getId(), apply.getId(), "失主取件完成（认领者）", currentUserId);

        // 转发人额外奖励1分：转发到寻物启事且最终认领成功
        Long forwarderId = item.getForwarderId();
        if (forwarderId != null
                && !forwarderId.equals(publisherId)
                && !forwarderId.equals(apply.getClaimerId())) {
            creditService.issueCreditOnce(forwarderId, 1, "FORWARD_REWARD",
                    item.getId(), apply.getId(), "招领转发至寻物启事，最终认领成功", currentUserId);
        }

        // 取件完成要通知发布人：东西确实交出去了、奖励到账了，不必自己刷物品状态
        User pickedUser = userMapper.selectById(apply.getClaimerId());
        String pickedName = pickedUser == null ? "认领人"
                : (pickedUser.getRealName() != null && !pickedUser.getRealName().isBlank()
                ? pickedUser.getRealName() : pickedUser.getUsername());
        DropPoint pickedPoint = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        String pickedPointName = pickedPoint == null || pickedPoint.getName() == null ? "投放点" : pickedPoint.getName();
        messageService.push(publisherId, "物品已取走 · 拾获奖励已到账",
                "你发布的「" + item.getTitle() + "」已由 " + pickedName + " 在「" + pickedPointName + "」确认取件，"
                        + "取件照片与签名已存档，拾获奖励 +3 已计入你的诚信分。"
                        + "如怀疑此人是冒领，可在物品详情发起「被冒领」申诉，系统会通知站点核对监控。",
                "CLAIM", item.getId(), apply.getId());

        return convertSingle(apply, item);
    }

    @Override
    @Transactional
    public void cancelPickup(Long id, Long currentUserId) {
        ClaimApply apply = getApplyOrThrow(id);
        FoundItem item = foundItemMapper.selectById(apply.getItemId());

        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        // 现场取消认领由失主本人操作
        if (!apply.getClaimerId().equals(currentUserId)) {
            throw new BusinessException(403, "只有认领人本人可以取消认领");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode())) {
            throw new BusinessException(409, "只有已同意待取件的申请可以取消");
        }
        // 物品已由站点处理（丢失待查/待归还/已作废/归档/已被放回公开池）时，现场取消不能再动物品状态，
        // 但也不能把申请人卡住：直接终止该条申请，让「待取件」闭环，物品状态完全保持原样
        if (!item.getItemStatus().equals(ItemStatus.CLAIMING.getCode())) {
            apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
            apply.setRejectCount(0);
            apply.setRejectReason("实物已由站点处理，本次认领终止（未发生交付）");
            claimApplyMapper.updateById(apply);

            messageService.push(apply.getClaimerId(), "本次认领已终止 · 实物已由站点处理",
                    "你待取件的「" + item.getTitle() + "」已由站点处理，当前不在交付流程中，"
                            + "本次认领予以终止：不生成纠纷、不扣诚信分，也不会发放任何奖励。"
                            + "若你已拿到与描述不符的物品，请在「我的认领」发起「物品不符」申诉。",
                    "CLAIM", item.getId(), apply.getId());
            return;
        }

        // 该条回复变2驳回，wrong_try_count清零（现场核对失败不视为恶意答错）
        apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
        apply.setRejectCount(0);
        claimApplyMapper.updateById(apply);

        // 排队锁（因他人被审核通过而临时锁定）一律作废：与「取件成功后其余锁定单全部作废」同一结论。
        // 物品退回公开后，谁想认领就重新提交，不把别人没被审核过的申请直接推回待审核。
        // 只有「答错满 3 次」的惩罚性锁定不属于排队，不能被一次取消放出，保持锁定并单独通知。
        LambdaQueryWrapper<ClaimApply> lockedWrapper = new LambdaQueryWrapper<>();
        lockedWrapper.eq(ClaimApply::getItemId, item.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode());
        List<ClaimApply> lockedList = claimApplyMapper.selectList(lockedWrapper);

        List<ClaimApply> voidedApplies = new java.util.ArrayList<>();
        List<ClaimApply> stillLocked = new java.util.ArrayList<>();
        List<Long> voidIds = new java.util.ArrayList<>();
        for (ClaimApply lockedApply : lockedList) {
            int rejectCount = lockedApply.getRejectCount() == null ? 0 : lockedApply.getRejectCount();
            if (rejectCount >= MAX_REJECT_COUNT) {
                stillLocked.add(lockedApply);
            } else {
                voidedApplies.add(lockedApply);
                voidIds.add(lockedApply.getId());
            }
        }
        if (!voidIds.isEmpty()) {
            LambdaQueryWrapper<ClaimApply> voidWrapper = new LambdaQueryWrapper<>();
            voidWrapper.in(ClaimApply::getId, voidIds);
            ClaimApply voidUpdate = new ClaimApply();
            voidUpdate.setApplyStatus(ClaimStatus.REJECTED.getCode());
            voidUpdate.setRejectReason("原认领人现场取消，物品已重新公开；本次申请作废，如需认领请重新提交");
            claimApplyMapper.update(voidUpdate, voidWrapper);
        }

        // 队列已清空，物品直接退回公开待认领
        item.setItemStatus(ItemStatus.PUBLIC.getCode());
        foundItemMapper.updateById(item);

        // 有东西重新回到「可认领/待审核」，必须让招领人知道，否则他会以为东西已经给出去了
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        User claimer = userMapper.selectById(currentUserId);
        String claimerName = claimer == null ? "认领人"
                : (claimer.getRealName() != null && !claimer.getRealName().isBlank()
                ? claimer.getRealName() : claimer.getUsername());
        DropPoint point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        String pointName = point == null || point.getName() == null ? "投放点" : point.getName();

        String stateForPublisher = "实物未离站，仍在「" + pointName + "」，该招领已重新回到「公开待认领」；"
                + "同物品其余排队申请已一并作废，有新申请时你会收到审核提醒。";
        String stateForPoint = "该招领已重新回到「公开待认领」，同物品其余排队申请已作废，可继续接收新申请。";

        messageService.push(publisherId, "取件人现场取消 · 招领待你重新审核",
                claimerName + " 在「" + pointName + "」取件时发现拿错了东西，已现场取消本次认领（不生成纠纷、不扣诚信分，"
                        + "也还没有发放任何奖励）。" + stateForPublisher + "物品归属不变，仍为你发布。",
                "CLAIM", item.getId(), apply.getId());

        if (point != null && point.getAdminId() != null) {
            messageService.push(point.getAdminId(), "取件人现场取消 · 实物未离站",
                    "招领「" + item.getTitle() + "」的取件人 " + claimerName + " 在现场核对后取消了认领，" +
                    "请确认实物仍在原存放位置、未被带走。" + stateForPoint,
                    "CLAIM", item.getId(), apply.getId());
        }

        // 被作废的排队申请人要知道：这不算他答错，也不会记驳回次数，想要就重新提交
        for (ClaimApply voided : voidedApplies) {
            messageService.push(voided.getClaimerId(), "你的认领申请已作废 · 物品重新公开",
                    "物品「" + item.getTitle() + "」的原认领人现场退回，实物未离站，该物品已重新回到「公开待认领」。"
                            + "你此前的排队申请已作废（不记驳回次数、不影响诚信分），如需认领请重新提交。",
                    "CLAIM", item.getId(), voided.getId());
        }

        // 仍在惩罚性锁定中的申请人也要说明清楚，别以为这次取消把自己也放出来了
        for (ClaimApply lockedApply : stillLocked) {
            int rejectCount = lockedApply.getRejectCount() == null ? 0 : lockedApply.getRejectCount();
            messageService.push(lockedApply.getClaimerId(), "你的认领申请仍处于锁定",
                    "物品「" + item.getTitle() + "」本次现场取消不影响你的锁定状态：你已因回答不符被驳回 "
                            + rejectCount + " 次，需自最后一次驳回起满 " + LOCK_HOURS + " 小时才能再次申请。",
                    "CLAIM", item.getId(), lockedApply.getId());
        }
    }

    @Override
    public IPage<ClaimApplyVO> listMy(Long currentUserId, int page, int size) {
        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApply::getClaimerId, currentUserId)
                .orderByDesc(ClaimApply::getCreatedAt);

        return convertPage(claimApplyMapper.selectPage(new Page<>(page, size), wrapper), null);
    }

    @Override
    public IPage<ClaimApplyVO> listForReview(Long currentUserId, Integer status, int page, int size) {
        User user = userMapper.selectById(currentUserId);
        if (user == null || (user.getRole() != UserRole.SYS_ADMIN && user.getRole() != UserRole.POINT_ADMIN)) {
            throw new BusinessException(403, "仅管理员可以查看待审核认领");
        }

        LambdaQueryWrapper<ClaimApply> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ClaimApply::getApplyStatus, status);
        }
        List<Long> itemIds = reviewableItemIds(user);
        if (itemIds != null) {
            if (itemIds.isEmpty()) {
                return new Page<>(page, size);
            }
            wrapper.in(ClaimApply::getItemId, itemIds);
        }
        wrapper.orderByDesc(ClaimApply::getCreatedAt);

        return convertPage(claimApplyMapper.selectPage(new Page<>(page, size), wrapper), null);
    }

    /**
     * 每日清理残留的锁定申请（与用户重新提交时的懒解锁保持同一套规则）：
     * 1. 物品仍在「认领中」且驳回不足 3 次 → 不动，正等取件；
     * 2. 物品已回到「公开」且驳回不足 3 次 → 作废（与现场取消、取件成功的结论一致，如需认领重新提交）；
     * 3. 物品已不可认领（取走/作废/丢失/归档等），或驳回满 3 次且过 24h → 关闭为「驳回」，
     *    满 3 次的把计数清零，并推送具体原因。
     */
    @Override
    @Transactional
    public int releaseStaleLocks() {
        List<ClaimApply> lockedApplies = claimApplyMapper.selectList(
                new LambdaQueryWrapper<ClaimApply>()
                        .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode()));
        if (lockedApplies.isEmpty()) {
            return 0;
        }

        List<Long> itemIds = lockedApplies.stream()
                .map(ClaimApply::getItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, FoundItem> itemMap = itemIds.isEmpty() ? Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, i -> i, (a, b) -> a));

        int handled = 0;
        for (ClaimApply apply : lockedApplies) {
            FoundItem item = apply.getItemId() == null ? null : itemMap.get(apply.getItemId());
            int rejectCount = apply.getRejectCount() == null ? 0 : apply.getRejectCount();
            int itemStatus = item == null || item.getItemStatus() == null ? -1 : item.getItemStatus();
            // 惩罚性锁定是否已满 24h（updatedAt 缺失按已到期处理，避免永久卡死）
            boolean lockExpired = apply.getUpdatedAt() == null
                    || !apply.getUpdatedAt().plusHours(LOCK_HOURS).isAfter(LocalDateTime.now());
            String itemTitle = item == null || item.getTitle() == null ? "该物品" : "「" + item.getTitle() + "」";

            // 情况 1：物品仍在认领中 → 正常等待取件，不动
            if (itemStatus == ItemStatus.CLAIMING.getCode()
                    && (rejectCount < MAX_REJECT_COUNT || !lockExpired)) {
                continue;
            }

            // 情况 2：物品回到公开、且不是惩罚性锁定 → 作废（与现场取消、取件成功保持同一结论）
            if (itemStatus == ItemStatus.PUBLIC.getCode() && rejectCount < MAX_REJECT_COUNT) {
                apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
                apply.setRejectReason("物品已重新公开，本次排队申请作废；如需认领请重新提交");
                claimApplyMapper.updateById(apply);
                messageService.push(apply.getClaimerId(), "你的认领申请已作废 · 物品重新公开",
                        itemTitle + "已重新回到「公开待认领」，你此前的排队申请已作废（不记驳回次数、不影响诚信分），"
                                + "如需认领请重新提交。",
                        "CLAIM", apply.getItemId(), apply.getId());
                handled++;
                continue;
            }

            // 情况 2'：物品回到公开，但驳回满 3 次且未满 24h → 惩罚期未结束，保持锁定
            if (itemStatus == ItemStatus.PUBLIC.getCode() && !lockExpired) {
                continue;
            }

            // 情况 3：关闭申请。先按物品真实状态给原因（用户更关心「东西去哪了」），
            // 只有物品本身仍可认领、纯粹是惩罚期到期时才用次数文案，避免「物品已被他人取走」却提示「可重新提交」
            String closeReason;
            if (rejectCount >= MAX_REJECT_COUNT) {
                apply.setRejectCount(0);
            }
            if (itemStatus == ItemStatus.PICKED_UP.getCode()) {
                closeReason = itemTitle + "已被他人取走，本次认领无法继续。若你到站发现拿到的是别人的东西，"
                        + "可在「我的认领」发起纠纷申诉。";
            } else if (itemStatus == ItemStatus.CLAIMING.getCode()) {
                closeReason = itemTitle + "已由他人认领审核通过（正待取件），你的申请已关闭；"
                        + "若对方到场后取消认领，物品会重新公开，可再次申请。";
            } else if (itemStatus == ItemStatus.VOIDED.getCode()) {
                closeReason = itemTitle + "已作废下架，不再开放认领。";
            } else if (itemStatus == ItemStatus.RETURNING.getCode()) {
                closeReason = itemTitle + "正在「待归还」处理中（纠纷核验或站点收回），本次认领已结束。";
            } else if (itemStatus == ItemStatus.LOST.getCode()) {
                closeReason = itemTitle + "已被标记为「丢失待查」，认领通道关闭。";
            } else if (itemStatus == ItemStatus.ARCHIVED.getCode()) {
                closeReason = itemTitle + "已归档，不再开放认领。";
            } else if (itemStatus == ItemStatus.EXPIRED.getCode()) {
                closeReason = itemTitle + "已超期下架，不再开放认领。";
            } else if (itemStatus == ItemStatus.PENDING_PLACEMENT.getCode()) {
                closeReason = itemTitle + "尚未正式投放，本次认领已结束。";
            } else if (itemStatus < 0) {
                closeReason = "关联物品已不存在，本次认领已关闭。";
            } else if (rejectCount >= MAX_REJECT_COUNT) {
                closeReason = "你已因回答不符被驳回 " + MAX_REJECT_COUNT + " 次，锁定 "
                        + LOCK_HOURS + " 小时已到期；本次申请已关闭，可重新提交并认真核对物品特征。";
            } else {
                closeReason = itemTitle + "当前不在认领流程中，本次认领已关闭。";
            }

            apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
            apply.setRejectReason("系统清理：物品已不可认领");
            claimApplyMapper.updateById(apply);
            messageService.push(apply.getClaimerId(), "认领申请已关闭", closeReason,
                    "CLAIM", apply.getItemId(), apply.getId());
            handled++;
        }

        return handled;
    }

    /**
     * 审核权限:发布者、系统管理员,或物品所在投放点的站点管理员。
     */
    private boolean canReviewClaim(FoundItem item, Long currentUserId) {
        if (item.getFounderId().equals(currentUserId)) {
            return true;
        }
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            return false;
        }
        if (user.getRole() == UserRole.SYS_ADMIN) {
            return true;
        }
        if (user.getRole() == UserRole.POINT_ADMIN && item.getDropPointId() != null) {
            DropPoint point = dropPointMapper.selectById(item.getDropPointId());
            return point != null && currentUserId.equals(point.getAdminId());
        }
        return false;
    }

    /**
     * 待审核列表范围:系统管理员看全部,站点管理员看自己站点物品+自己发布的物品。
     */
    private List<Long> reviewableItemIds(User user) {
        if (user.getRole() == UserRole.SYS_ADMIN) {
            return null;
        }
        List<Long> myPointIds = dropPointMapper.selectList(
                new LambdaQueryWrapper<DropPoint>().eq(DropPoint::getAdminId, user.getId()))
                .stream().map(DropPoint::getId).toList();
        LambdaQueryWrapper<FoundItem> itemWrapper = new LambdaQueryWrapper<>();
        if (myPointIds.isEmpty()) {
            itemWrapper.eq(FoundItem::getFounderId, user.getId());
        } else {
            itemWrapper.and(w -> w.in(FoundItem::getDropPointId, myPointIds)
                    .or().eq(FoundItem::getFounderId, user.getId()));
        }
        return foundItemMapper.selectList(itemWrapper)
                .stream().map(FoundItem::getId).toList();
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
                        .collect(Collectors.toMap(User::getId,
                                u -> u.getRealName() == null ? "" : u.getRealName(), (a, b) -> a));

        // 投放点与站点负责人联系方式（认领人到站前可先电话核对）
        List<Long> pointIds = itemMap.values().stream()
                .map(FoundItem::getDropPointId).filter(Objects::nonNull).distinct().toList();
        List<DropPoint> points = pointIds.isEmpty() ? List.of() : dropPointMapper.selectBatchIds(pointIds);
        Map<Long, DropPoint> pointMap = points.stream()
                .collect(Collectors.toMap(DropPoint::getId, p -> p, (a, b) -> a));
        List<Long> pointAdminIds = points.stream()
                .map(DropPoint::getAdminId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> pointAdmins = pointAdminIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(pointAdminIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return applies.stream()
                .map(apply -> {
                    FoundItem item = knownItem != null ? knownItem : itemMap.get(apply.getItemId());
                    ClaimApplyVO vo = ClaimApplyVO.fromEntity(
                            apply,
                            item != null ? item.getTitle() : null,
                            item != null ? item.getClaimQuestion() : null,
                            claimerNames.get(apply.getClaimerId()));
                    if (item != null && item.getDropPointId() != null) {
                        DropPoint point = pointMap.get(item.getDropPointId());
                        if (point != null) {
                            vo.setDropPointId(point.getId());
                            vo.setDropPointName(point.getName());
                            vo.setDropPointLocation(point.getLocation());
                            User admin = point.getAdminId() == null ? null : pointAdmins.get(point.getAdminId());
                            if (admin != null) {
                                vo.setDropPointAdminName(admin.getRealName());
                                vo.setDropPointAdminPhone(admin.getPhone());
                            }
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private ClaimApplyVO convertSingle(ClaimApply apply, FoundItem item) {
        User claimer = userMapper.selectById(apply.getClaimerId());
        ClaimApplyVO vo = ClaimApplyVO.fromEntity(
                apply,
                item != null ? item.getTitle() : null,
                item != null ? item.getClaimQuestion() : null,
                claimer != null ? claimer.getRealName() : null);
        if (item != null && item.getDropPointId() != null) {
            DropPoint point = dropPointMapper.selectById(item.getDropPointId());
            if (point != null) {
                vo.setDropPointId(point.getId());
                vo.setDropPointName(point.getName());
                vo.setDropPointLocation(point.getLocation());
                if (point.getAdminId() != null) {
                    User admin = userMapper.selectById(point.getAdminId());
                    if (admin != null) {
                        vo.setDropPointAdminName(admin.getRealName());
                        vo.setDropPointAdminPhone(admin.getPhone());
                    }
                }
            }
        }
        return vo;
    }

    private ClaimApply getApplyOrThrow(Long id) {
        ClaimApply apply = claimApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(404, "认领申请不存在");
        }
        return apply;
    }
}
