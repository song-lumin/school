package com.school.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.lostfound.dto.DisputeCreateRequest;
import com.school.lostfound.dto.DisputeHandleRequest;
import com.school.lostfound.dto.PointCheckRequest;
import com.school.lostfound.entity.ClaimApply;
import com.school.lostfound.entity.CreditLog;
import com.school.lostfound.entity.Dispute;
import com.school.lostfound.entity.DropPoint;
import com.school.lostfound.entity.FoundItem;
import com.school.lostfound.entity.User;
import com.school.lostfound.enums.ClaimStatus;
import com.school.lostfound.enums.ItemStatus;
import com.school.lostfound.enums.UserRole;
import com.school.lostfound.exception.BusinessException;
import com.school.lostfound.mapper.ClaimApplyMapper;
import com.school.lostfound.mapper.CreditLogMapper;
import com.school.lostfound.mapper.DisputeMapper;
import com.school.lostfound.mapper.DropPointMapper;
import com.school.lostfound.mapper.FoundItemMapper;
import com.school.lostfound.mapper.UserMapper;
import com.school.lostfound.service.CreditService;
import com.school.lostfound.service.DisputeService;
import com.school.lostfound.service.MessageService;
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
    /** 物品损坏（领取后发现物品被损坏或存在未披露的破损） */
    public static final String TYPE_ITEM_DAMAGED = "ITEM_DAMAGED";
    public static final String TYPE_OTHER = "OTHER";
    /** 物品被冒领（失主发起，无时间窗口） */
    public static final String TYPE_FALSE_CLAIM = "FALSE_CLAIM";
    /** 找不到物品（待取件状态下由认领人申报，交站点核查） */
    public static final String TYPE_ITEM_NOT_FOUND = "ITEM_NOT_FOUND";

    /** 找不到物品工单的核查结论 */
    public static final String RESULT_FOUND = "FOUND";                 // 实物在站，属误报
    public static final String RESULT_NOT_DELIVERED = "NOT_DELIVERED"; // 从未实际投放（虚假招领）
    public static final String RESULT_STOLEN = "STOLEN";               // 实物被他人拿走

    /** 损坏件的定责方（决定谁担责、谁的积分回滚） */
    public static final String FAULT_SITE = "SITE_FAULT";              // 站点保管/流转损坏（默认）
    public static final String FAULT_CLAIMER = "CLAIMER_FAULT";        // 认领人取件后使用/搬运不当
    public static final String FAULT_PUBLISHER = "PUBLISHER_FAULT";    // 发布人隐瞒破损或描述严重失实

    /** 虚假投放（未实际投放实物）的诚信积分处罚分 */
    private static final int FALSE_PUBLISH_PENALTY = 2;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    /** 处理中：站点已接手、已回填，但还没出最终结论（例如选了「其他」转管理员，或等监控回放） */
    private static final int STATUS_PROCESSING = 3;

    private static final Duration DISPUTE_SUBMIT_WINDOW = Duration.ofDays(7);

    /** 恶意冒领的诚信积分处罚分 */
    private static final int FALSE_CLAIM_PENALTY = 5;

    /** 站点回填现场核验的 SLA：超过该小时数未回填即催办 */
    public static final int SLA_POINT_CHECK_HOURS = 24;
    /** 同一工单最多催办次数，避免持续骚扰 */
    private static final int MAX_POINT_REMIND = 3;

    private final DisputeMapper disputeMapper;
    private final ClaimApplyMapper claimApplyMapper;
    private final FoundItemMapper foundItemMapper;
    private final CreditLogMapper creditLogMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    private final MessageService messageService;
    private final DropPointMapper dropPointMapper;

    @Override
    @Transactional
    public DisputeVO createDispute(DisputeCreateRequest request, Long currentUserId) {
        String type = request.getDisputeType();
        if (!TYPE_ITEM_MISMATCH.equals(type) && !TYPE_ITEM_DAMAGED.equals(type)
                && !TYPE_OTHER.equals(type) && !TYPE_FALSE_CLAIM.equals(type)
                && !TYPE_ITEM_NOT_FOUND.equals(type)) {
            throw new BusinessException(400, "无效的纠纷类型");
        }

        if (TYPE_FALSE_CLAIM.equals(type)) {
            return createFalseClaimDispute(request, currentUserId);
        }

        if (TYPE_ITEM_NOT_FOUND.equals(type)) {
            return createItemNotFoundDispute(request, currentUserId);
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
            throw new BusinessException(409, "申诉窗口已关闭（仅限领取后7天内提交）");
        }

        FoundItem item = foundItemMapper.selectById(apply.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        // 同一笔认领 + 同一类型 只允许一张待处理工单：否则可重复提交，
        // 两条都通过时误领次数会算两次、站点也被重复通知（「找不到物品」已有去重，这里补齐不符/损坏/其他）
        long samePending = disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>()
                .eq(Dispute::getApplyId, apply.getId())
                .eq(Dispute::getDisputeType, type)
                .in(Dispute::getStatus, STATUS_PENDING, STATUS_PROCESSING));
        if (samePending > 0) {
            throw new BusinessException(409,
                    "该认领订单已有待处理的「" + disputeTypeLabel(type) + "」工单，请等待处理结果，勿重复提交");
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

        notifyPointAdminOnDispute(dispute, apply, item);

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

        // 冒领申诉同样必须立刻送到站点：核交付凭证、调监控只能由站点做，等管理员来问就晚了
        notifyPointAdminOnFalseClaim(dispute, item, apply, currentUserId);

        log.info("冒领申诉已提交：disputeId={}, itemId={}, applyId={}, applicant={}",
                dispute.getId(), item.getId(), apply.getId(), currentUserId);
        return toVO(dispute, item.getTitle());
    }

    /**
     * 找不到物品：认领人已通过审核、到投放点却找不到实物时申报，交站点/管理员核查。
     * 不影响物品与申请状态（避免虚报卡死流程），核查结论由处理人三选一。
     */
    private DisputeVO createItemNotFoundDispute(DisputeCreateRequest request, Long currentUserId) {
        if (request.getApplyId() == null) {
            throw new BusinessException(400, "认领申请ID不能为空");
        }
        ClaimApply apply = claimApplyMapper.selectById(request.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "认领申请不存在");
        }
        if (!apply.getClaimerId().equals(currentUserId)) {
            throw new BusinessException(403, "只有认领人本人可以申报找不到物品");
        }
        if (!apply.getApplyStatus().equals(ClaimStatus.APPROVED_WAITING_PICKUP.getCode())) {
            throw new BusinessException(409, "只有「已通过待取件」的认领订单才能申报找不到物品");
        }

        FoundItem item = foundItemMapper.selectById(apply.getItemId());
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }

        long pending = disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>()
                .eq(Dispute::getApplyId, apply.getId())
                .eq(Dispute::getDisputeType, TYPE_ITEM_NOT_FOUND)
                .in(Dispute::getStatus, STATUS_PENDING, STATUS_PROCESSING));
        if (pending > 0) {
            throw new BusinessException(409, "该认领订单已有待核查的「找不到物品」工单");
        }

        Dispute dispute = new Dispute();
        dispute.setApplicantId(currentUserId);
        dispute.setApplyId(apply.getId());
        dispute.setItemId(item.getId());
        dispute.setDisputeType(TYPE_ITEM_NOT_FOUND);
        dispute.setDescription(request.getDescription());
        dispute.setEvidenceImages(request.getEvidenceImages());
        dispute.setStatus(STATUS_PENDING);
        disputeMapper.insert(dispute);

        log.info("找不到物品工单已提交，待站点核查：disputeId={}, applyId={}, itemId={}, applicant={}",
                dispute.getId(), apply.getId(), item.getId(), currentUserId);

        notifyPointAdminOnItemNotFound(dispute, apply, item, currentUserId);

        return toVO(dispute, item.getTitle());
    }

    /**
     * 不符 / 损坏 / 其他纠纷：提交后即通知该物品所属投放点负责人。
     * 线上定责是系统管理员的事，线下核验（找实物、调监控、核交付凭证）必须由站点做，
     * 因此工单一提交站点就要收到，而不是等管理员来问。
     */
    private void notifyPointAdminOnDispute(Dispute dispute, ClaimApply apply, FoundItem item) {
        DropPoint point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        if (point == null || point.getAdminId() == null) {
            log.info("投放点无负责管理员，纠纷工单仅进入系统管理员队列：disputeId={}", dispute.getId());
            return;
        }
        User claimer = userMapper.selectById(apply.getClaimerId());
        String claimerName = claimer == null ? "认领人" : displayName(claimer);
        String claimerPhone = claimer != null && StringUtils.hasText(claimer.getPhone()) ? claimer.getPhone() : "未填写";
        String label = disputeTypeLabel(dispute.getDisputeType());

        // 「不符」和「损坏」是两件事，站点要核的东西不同：不符查「是不是这件」，损坏查「什么时候坏的」。
        // 混成一句会让站点只拍照不回填，定责就缺少依据。
        String checkAction;
        if (TYPE_ITEM_MISMATCH.equals(dispute.getDisputeType())) {
            checkAction = "请按招领填写的品名、颜色、特征逐项比对货架实物，判断是认领人拿错还是招领描述写错，并把实物现状拍照留档；"
                    + "定责后实物会转「待归还」，请勿直接放回公开货架；";
        } else if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())) {
            checkAction = "请到存放区确认损坏部位与程度并拍照，同时核对交接入库时是否已有损坏记录，用于区分保管责任与认领人责任；"
                    + "损坏件定责后作废、不再重新公开，请勿放回公开货架；";
        } else {
            checkAction = "请核对实物现状与交付凭证，必要时走「监控调取」流程取交付画面；";
        }

        String content = String.format(
                "【%s】%s 就认领物品《%s》提交了工单#%d，说明：%s（认领人电话 %s）。"
                        + "%s"
                        + "然后在「纠纷核验」中回填现场事实。定责与积分由系统管理员依据你的陈述处理，站点不写结论。",
                label, claimerName, item.getTitle(), dispute.getId(),
                dispute.getDescription() == null ? "未填写" : dispute.getDescription(), claimerPhone, checkAction);

        messageService.push(point.getAdminId(), "站点核验提醒：" + label, content,
                "DISPUTE", item.getId(), apply.getId());
    }

    /**
     * 提交后通知投放点管理员：附认领人姓名与联系方式，便于站点现场核对并回电。
     * 无投放点或无负责管理员时不报错，工单仍会进入系统管理员的申诉工单队列。
     */
    private void notifyPointAdminOnItemNotFound(Dispute dispute, ClaimApply apply, FoundItem item, Long claimerId) {        DropPoint point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        String pointName = point != null ? point.getName() : "未指定投放点";
        User claimer = userMapper.selectById(claimerId);
        String claimerName = claimer == null ? "认领人"
                : (StringUtils.hasText(claimer.getRealName()) ? claimer.getRealName() : claimer.getUsername());
        String claimerPhone = claimer != null && StringUtils.hasText(claimer.getPhone()) ? claimer.getPhone() : "未填写";

        String content = String.format(
                "【找不到物品】%s 反映在「%s」没有找到认领物品《%s》（工单#%d）。认领人联系方式：%s。"
                        + "请到站点存放区核对实物后，在「申诉工单」中选择核查结论：实物在站 / 从未实际投放 / 被他人拿走。"
                        + "如需联系认领人可直接拨打上述号码。",
                claimerName, pointName, item.getTitle(), dispute.getId(), claimerPhone);

        if (point != null && point.getAdminId() != null) {
            messageService.push(point.getAdminId(), "站点核查提醒：有人反映找不到物品", content,
                    "ITEM_NOT_FOUND", item.getId(), apply.getId());
        } else {
            log.info("投放点无负责管理员，找不到物品工单仅进入申诉队列：disputeId={}", dispute.getId());
        }
    }

    /**
     * 冒领申诉：提交后立即通知物品所属投放点负责人，附申诉人姓名电话与需核对的内容。
     * 站点要做的不是定责，而是找证据：交付画面（监控）、当时的签字合影、交接记录。
     */
    private void notifyPointAdminOnFalseClaim(Dispute dispute, FoundItem item, ClaimApply apply, Long applicantId) {
        DropPoint point = item.getDropPointId() == null ? null : dropPointMapper.selectById(item.getDropPointId());
        if (point == null || point.getAdminId() == null) {
            log.info("投放点无负责管理员，冒领工单仅进入系统管理员队列：disputeId={}", dispute.getId());
        }
        User applicant = userMapper.selectById(applicantId);
        String applicantName = applicant == null ? "申请人" : displayName(applicant);
        String applicantPhone = applicant != null && StringUtils.hasText(applicant.getPhone())
                ? applicant.getPhone() : "未填写";

        String content = String.format(
                "【物品被冒领】%s 声称物品《%s》系其所有，指认 %04d 次认领（工单#%d）为冒领。申诉人联系方式：%s。"
                        + "请到站点核对该笔交付的现场凭证（签字合影、交付照片、交接记录），有监控的请走「监控调取」取交付画面，"
                        + "然后在「纠纷核验」回填事实。是否成立由系统管理员依据你的陈述定责。",
                applicantName, item.getTitle(), dispute.getApplyId() == null ? 0L : dispute.getApplyId(),
                dispute.getId(), applicantPhone);

        String title = "站点核查提醒：物品被冒领（工单#" + dispute.getId() + "）";
        if (point != null && point.getAdminId() != null) {
            messageService.push(point.getAdminId(), title, content,
                    "DISPUTE", item.getId(), apply.getId());
        }
        // 同时告知实际取件人：有人指认其冒领，留出说明机会，避免结论单方面成立
        if (apply != null && apply.getClaimerId() != null) {
            messageService.push(apply.getClaimerId(), "有人指认你冒领 · 请准备说明",
                    "有人就物品「" + item.getTitle() + "」的本次领取提出「被冒领」申诉（工单#" + dispute.getId()
                            + "）。系统管理员会调取站点交付凭证与监控核实，你可补充当时取件的情况；工单结论会另行通知你。",
                    "DISPUTE", item.getId(), apply.getId());
        }
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

        Page<DisputeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<DisputeVO> vos = result.getRecords().stream()
                .map(d -> toVO(d, null))
                .collect(Collectors.toList());
        fillRelations(vos);
        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 站点管理员视角：只返回自己负责投放点上的物品所涉工单。
     * 站点只做事实核验，定责仍由系统管理员完成。
     */
    @Override
    public IPage<DisputeVO> listForPoint(Long pointAdminId, String disputeType, Integer status, int page, int size) {
        Page<DisputeVO> emptyPage = new Page<>(page, size, 0);
        emptyPage.setRecords(List.of());

        // 系统管理员是站点管理员的超集：不按「自己负责的点位」过滤，直接看到全部点位工单；
        // 点位管理员仍只看自己负责的点位。站点是兼职岗，管理员代看、代核验时需要这个视角。
        User operator = userMapper.selectById(pointAdminId);
        boolean isSysAdmin = operator != null
                && operator.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN;
        List<Long> itemIds = null;
        if (!isSysAdmin) {
            List<DropPoint> myPoints = dropPointMapper.selectList(new LambdaQueryWrapper<DropPoint>()
                    .eq(DropPoint::getAdminId, pointAdminId));
            if (myPoints.isEmpty()) {
                return emptyPage;
            }
            List<Long> pointIds = myPoints.stream().map(DropPoint::getId).toList();
            List<FoundItem> myItems = foundItemMapper.selectList(new LambdaQueryWrapper<FoundItem>()
                    .in(FoundItem::getDropPointId, pointIds));
            if (myItems.isEmpty()) {
                return emptyPage;
            }
            itemIds = myItems.stream().map(FoundItem::getId).toList();
        }

        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (itemIds != null) {
            wrapper.in(Dispute::getItemId, itemIds);
        }
        if (StringUtils.hasText(disputeType)) {
            wrapper.eq(Dispute::getDisputeType, disputeType);
        }
        if (status != null) {
            wrapper.eq(Dispute::getStatus, status);
        }
        wrapper.orderByAsc(Dispute::getStatus).orderByDesc(Dispute::getCreatedAt);

        IPage<Dispute> result = disputeMapper.selectPage(new Page<>(page, size), wrapper);
        Page<DisputeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<DisputeVO> vos = result.getRecords().stream()
                .map(d -> toVO(d, null))
                .collect(Collectors.toList());
        fillRelations(vos);
        voPage.setRecords(vos);
        return voPage;
    }

    /**
     * 回填现场事实。首选本点位负责人回填；但站点是兼职岗、不一定在岗，
     * 等不到人时系统管理员可电话核实后代填，代填会写进核验人字段并反向通知点位负责人留痕。
     * 只存事实与核验人，不改状态、不动积分。
     */
    @Override
    @Transactional
    public void pointCheck(Long id, PointCheckRequest request, Long operatorId) {
        Dispute dispute = getDisputeOrThrow(id);
        if (!dispute.getStatus().equals(STATUS_PENDING) && !dispute.getStatus().equals(STATUS_PROCESSING)) {
            throw new BusinessException(409, "该工单已处理完毕，无需再处理");
        }
        if (dispute.getItemId() == null) {
            throw new BusinessException(409, "该工单未关联物品，无法做站点核验");
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null || item.getDropPointId() == null) {
            throw new BusinessException(409, "物品未关联投放点，无法做站点核验");
        }
        DropPoint point = dropPointMapper.selectById(item.getDropPointId());
        if (point == null) {
            throw new BusinessException(409, "投放点不存在，无法做站点核验");
        }

        User operator = userMapper.selectById(operatorId);
        boolean isSysAdmin = operator != null
                && operator.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN;
        boolean isPointOwner = point.getAdminId() != null && point.getAdminId().equals(operatorId);
        if (!isSysAdmin && !isPointOwner) {
            throw new BusinessException(403, "只有该物品所属投放点的负责人（或系统管理员代填）可以回填现场核验");
        }

        dispute.setPointCheckNote(request.getNote().trim());
        dispute.setPointCheckerId(operatorId);
        dispute.setPointCheckedAt(LocalDateTime.now());
        disputeMapper.updateById(dispute);

        // 站点只负责核对、调监控、联系沟通：这里只落事实，不预设结论、不碰积分，
        // 由系统管理员在「纠纷管理」里勾选要回滚哪几笔已发放的分并填写原因。

        if (isPointOwner) {
            messageService.push(dispute.getApplicantId(), "站点已完成现场核验（工单#" + dispute.getId() + "）",
                    "「" + point.getName() + "」负责人已提交现场核验情况：" + dispute.getPointCheckNote()
                            + "。系统管理员将据此定责，结论处理后会再通知你。",
                    "DISPUTE", dispute.getItemId(), dispute.getApplyId());
        } else {
            String standInName = operator == null ? "系统管理员" : displayName(operator);
            messageService.push(dispute.getApplicantId(), "已完成现场核验（工单#" + dispute.getId() + "）",
                    "站点负责人未在岗，由" + standInName + "电话核实「" + point.getName()
                            + "」实物情况后回填：" + dispute.getPointCheckNote()
                            + "。系统管理员将据此定责，结论处理后会再通知你。",
                    "DISPUTE", dispute.getItemId(), dispute.getApplyId());
            if (point.getAdminId() != null) {
                messageService.push(point.getAdminId(), "现场核验已由系统代填（工单#" + dispute.getId() + "）",
                        standInName + "因你未在岗，已电话核实后回填现场核验：" + dispute.getPointCheckNote()
                                + "。如事实有出入，请在该工单下补充说明。",
                        "DISPUTE", dispute.getItemId(), dispute.getApplyId());
            }
        }

        // 站点反馈到位即转「处理中」，并明确喊系统管理员来定责（积分由管理员勾选回滚）
        dispute.setStatus(STATUS_PROCESSING);
        disputeMapper.updateById(dispute);
        for (User admin : userMapper.selectList(null)) {
            if (admin.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                messageService.push(admin.getId(), "站点已反馈现场情况，请定责（工单#" + dispute.getId() + "）",
                        "「" + point.getName() + "」已完成核对（状态：处理中）：" + dispute.getPointCheckNote()
                                + "。请到「纠纷管理」勾选要回滚的积分项、填写定责原因。",
                        "DISPUTE", dispute.getItemId(), dispute.getApplyId());
            }
        }

        log.info("现场核验回填：disputeId={}, pointId={}, checker={}, standIn={}",
                dispute.getId(), point.getId(), operatorId, !isPointOwner);
    }

    /**
     * 统一补齐工单关联信息：申请人、物品标题、投放点与负责人电话、站点核验人。
     */
    private void fillRelations(List<DisputeVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> itemIds = vos.stream().map(DisputeVO::getItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, FoundItem> items = itemIds.isEmpty() ? Map.of()
                : foundItemMapper.selectBatchIds(itemIds).stream()
                        .collect(Collectors.toMap(FoundItem::getId, i -> i));

        List<Long> pointIds = items.values().stream()
                .map(FoundItem::getDropPointId).filter(Objects::nonNull).distinct().toList();
        Map<Long, DropPoint> points = pointIds.isEmpty() ? Map.of()
                : dropPointMapper.selectBatchIds(pointIds).stream()
                        .collect(Collectors.toMap(DropPoint::getId, p -> p));

        java.util.Set<Long> userIds = new java.util.HashSet<>();
        vos.forEach(v -> {
            if (v.getApplicantId() != null) userIds.add(v.getApplicantId());
            if (v.getPointCheckerId() != null) userIds.add(v.getPointCheckerId());
        });
        points.values().forEach(p -> {
            if (p.getAdminId() != null) userIds.add(p.getAdminId());
        });
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        for (DisputeVO vo : vos) {
            FoundItem item = items.get(vo.getItemId());
            Long pointAdminId = null;
            if (item != null) {
                vo.setItemTitle(item.getTitle());
                DropPoint point = item.getDropPointId() == null ? null : points.get(item.getDropPointId());
                if (point != null) {
                    pointAdminId = point.getAdminId();
                    vo.setDropPointId(point.getId());
                    vo.setDropPointName(point.getName());
                    User pointAdmin = point.getAdminId() == null ? null : users.get(point.getAdminId());
                    if (pointAdmin != null) {
                        vo.setDropPointAdminName(displayName(pointAdmin));
                        vo.setDropPointAdminPhone(pointAdmin.getPhone());
                    }
                }
            }
            User applicant = users.get(vo.getApplicantId());
            if (applicant != null) {
                vo.setApplicantName(displayName(applicant));
                vo.setApplicantPhone(applicant.getPhone());
            }
            User checker = users.get(vo.getPointCheckerId());
            if (checker != null) {
                vo.setPointCheckerName(displayName(checker));
            }

            // 站点不在岗的可见线索：等待时长 + 是否被代填
            if (vo.getStatus() != null && vo.getStatus() == STATUS_PENDING && vo.getCreatedAt() != null) {
                long hours = Duration.between(vo.getCreatedAt(), LocalDateTime.now()).toHours();
                vo.setPendingHours((int) Math.max(0, hours));
            }
            // SLA 判红：待处理、站点还没回填、已超过 24h
            if (vo.getStatus() != null && vo.getStatus() == STATUS_PENDING
                    && vo.getPointCheckedAt() == null
                    && vo.getPendingHours() != null && vo.getPendingHours() >= SLA_POINT_CHECK_HOURS) {
                vo.setPointSlaOverdue(true);
            }
            if (vo.getPointCheckerId() != null) {
                vo.setPointCheckStandIn(pointAdminId == null || !pointAdminId.equals(vo.getPointCheckerId()));
            }
        }
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    /**
     * 回避校验：定责人若正是该物品所属投放点的负责人，等于自己审自己，直接拒绝。
     */
    private void assertNoSelfReview(Dispute dispute, Long operatorId) {
        if (dispute.getItemId() == null) {
            return;
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null || item.getDropPointId() == null) {
            return;
        }
        DropPoint point = dropPointMapper.selectById(item.getDropPointId());
        if (point != null && point.getAdminId() != null && point.getAdminId().equals(operatorId)) {
            throw new BusinessException(409, "你是该物品所属投放点「" + point.getName()
                    + "」的负责人，本工单需回避，请转交其他管理员定责");
        }
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
        DisputeVO vo = toVO(dispute, itemTitle);
        // 单条查询同样补齐站点、核验人与等待时长，代填后前端刷新才能立刻看到
        fillRelations(List.of(vo));
        return vo;
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
        handleInternal(id, request, operatorId, false);
    }

    /**
     * 定责执行体，管理员定责与站点现场结案共用同一套状态/积分处理。
     * fromSiteCheck=true 表示站点刚做完现场核验直接出结论：这本来就是点位负责人的职责，
     * 此时不做「点位负责人回避」，否则任何工单都无法在本点位结案。
     */
    private void handleInternal(Long id, DisputeHandleRequest request, Long operatorId, boolean fromSiteCheck) {
        Dispute dispute = getDisputeOrThrow(id);
        if (!dispute.getStatus().equals(STATUS_PENDING) && !dispute.getStatus().equals(STATUS_PROCESSING)) {
            throw new BusinessException(409, "该工单已处理，请勿重复操作");
        }
        if (!fromSiteCheck) {
            assertNoSelfReview(dispute, operatorId);
        }

        // 管理员勾选了回滚项（哪怕是空数组）就走手动定责：不按纠纷类型预设，勾哪笔退哪笔
        boolean manual = request.getRollbackItems() != null;
        if (manual) {
            dispute.setStatus(request.getApproved() ? STATUS_APPROVED : STATUS_REJECTED);
            handleManualSettle(dispute, request, operatorId);
        } else if (request.getApproved()) {
            dispute.setStatus(STATUS_APPROVED);
            if (TYPE_FALSE_CLAIM.equals(dispute.getDisputeType())) {
                handleFalseClaimApproval(dispute, request, operatorId);
            } else if (TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())) {
                handleItemNotFound(dispute, request, operatorId);
            } else {
                handleDisputeApproval(dispute, request, operatorId);
            }
        } else {
            dispute.setStatus(STATUS_REJECTED);
        }

        // 站点没回填就定责：强制标记「站点未响应」并记录标记时间，谁也不能把这笔失职抹掉
        LocalDateTime handledAt = LocalDateTime.now();
        if (!StringUtils.hasText(dispute.getPointCheckNote())) {
            dispute.setSiteUnresponsive(1);
            dispute.setSiteUnresponsiveAt(handledAt);
            long waited = dispute.getCreatedAt() == null ? 0
                    : Duration.between(dispute.getCreatedAt(), handledAt).toHours();
            log.warn("定责时站点仍未回填现场核验，已标记站点未响应：disputeId={}, 已等待={}h, 据点={}",
                    dispute.getId(), waited, dispute.getItemId());
        }
        dispute.setHandlerNote(request.getHandlerNote());
        dispute.setHandlerId(operatorId);
        dispute.setHandledAt(handledAt);
        disputeMapper.updateById(dispute);

        // 结论必须回到线下执行方：站点据此交件/归档/等实物回站，否则线下环节断链
        notifyPointAdminResult(dispute, request.getApproved());
        // 结论也必须回到申诉人本人：之前只有站点收到，申诉人被驳回还是通过都不知情
        notifyApplicantResult(dispute, request.getApproved());
    }

    /**
     * 手动定责：系统管理员自己勾选这张单要撤回哪几笔已发放的分，不预设纠纷类型结论。
     * 每笔按 (人 + 类型 + 物品 + 认领申请) 幂等，没发放过的自动跳过；物品处置由管理员另选，不选就不动。
     */
    private void handleManualSettle(Dispute dispute, DisputeHandleRequest request, Long operatorId) {
        if (dispute.getItemId() == null) {
            throw new BusinessException(400, "该工单未关联物品，无法处理");
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null) {
            throw new BusinessException(404, "关联物品不存在");
        }
        ClaimApply apply = dispute.getApplyId() == null ? null : claimApplyMapper.selectById(dispute.getApplyId());
        String reason = "纠纷工单#" + dispute.getId() + "：" + request.getHandlerNote();
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        List<String> picked = request.getRollbackItems();

        if (picked.contains("PUBLISH_CHECK")) {
            rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
        }
        if (picked.contains("PUBLISH_PICKUP")) {
            if (apply == null) {
                throw new BusinessException(400, "该工单没有关联认领申请，无法回滚「招领方·认领分 +3」");
            }
            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        }
        if (picked.contains("FORWARD")) {
            if (apply == null) {
                throw new BusinessException(400, "该工单没有关联认领申请，无法回滚「转发方·转发分 +1」");
            }
            rollbackForwardReward(item, apply, reason, operatorId);
        }
        if (picked.contains("CLAIMER")) {
            if (apply == null) {
                throw new BusinessException(400, "该工单没有关联认领申请，无法回滚「认领方·认领分 +1」");
            }
            rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
        }

        String dispose = request.getItemDispose() == null ? "" : request.getItemDispose().trim().toUpperCase();
        if (!dispose.isBlank() && !"NONE".equals(dispose)) {
            switch (dispose) {
                case "VOIDED" -> item.setItemStatus(ItemStatus.VOIDED.getCode());
                case "LOST" -> item.setItemStatus(ItemStatus.LOST.getCode());
                case "RETURNING" -> item.setItemStatus(ItemStatus.RETURNING.getCode());
                case "PUBLIC" -> item.setItemStatus(ItemStatus.PUBLIC.getCode());
                default -> throw new BusinessException(400, "未知的物品处置方式：" + dispose);
            }
            foundItemMapper.updateById(item);
            log.info("手动定责物品处置：disputeId={}, itemId={}, dispose={}", dispute.getId(), item.getId(), dispose);
        }
        log.info("手动定责完成：disputeId={}, 勾选回滚项={}, 物品处置={}",
                dispute.getId(), picked, dispose.isBlank() ? "NONE" : dispose);
    }

    /**
     * 通知申诉人定责结论：通过要说清物品/积分怎么处置、自己下一步做什么；
     * 驳回也要说清「为什么」，并明确物品状态未变化，避免申诉人以为东西已经被收回。
     */
    private void notifyApplicantResult(Dispute dispute, boolean approved) {
        if (dispute.getApplicantId() == null) {
            return;
        }
        String label = disputeTypeLabel(dispute.getDisputeType());
        String title = approved ? "申诉已通过 · " + label : "申诉未通过 · " + label;
        String handleNote = dispute.getHandlerNote() == null || dispute.getHandlerNote().isBlank()
                ? "未填写" : dispute.getHandlerNote();

        StringBuilder sb = new StringBuilder();
        sb.append("你提交的工单#").append(dispute.getId()).append("（").append(label).append("）已处理完毕。");

        if (!approved) {
            sb.append("结论：申诉不成立（已驳回）。处理意见：").append(handleNote)
                    .append("。物品状态与积分均未发生任何变化，无需额外操作；")
                    .append("如你仍有新证据，可重新提交工单（同一订单同一类型同时只允许一张待处理）。");
        } else if (TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())) {
            String result = dispute.getHandleResult();
            if (RESULT_FOUND.equals(result)) {
                sb.append("结论：实物在站（误报）。处理意见：").append(handleNote)
                        .append("。你的认领资格与诚信分均未受影响，请回站点重新核对后上传签字合影完成领取。");
            } else if (RESULT_STOLEN.equals(result)) {
                sb.append("结论：实物已被他人拿走。处理意见：").append(handleNote)
                        .append("。物品已转「丢失待查」，发布人不担责，你的取件资格保留，站点找回后会通知你。");
            } else {
                sb.append("结论：从未实际投放（虚假招领）。处理意见：").append(handleNote)
                        .append("。该招领已作废，本单终止；你无责、不扣分，可重新浏览其他失物。");
            }
        } else if (TYPE_FALSE_CLAIM.equals(dispute.getDisputeType())) {
            sb.append("结论：冒领成立。处理意见：").append(handleNote)
                    .append("。该次认领已作废：冒领人的取件奖励 +1 已回滚，并被扣诚信分 5 分、计一次误领；")
                    .append("发布人的拾获与交物奖励不回收（非其过错）。")
                    .append("物品将转「待归还」，站点收回后重新公开待认领，届时你可重新认领。");
        } else if (TYPE_ITEM_MISMATCH.equals(dispute.getDisputeType())) {
            sb.append("结论：物品不符（误领）成立。处理意见：").append(handleNote)
                    .append("。本次误领计你 1 次，不扣诚信分；本次取件奖励 +1 已回滚，发布人的拾获与交物奖励保留。")
                    .append("物品已转「待归还」，请把实物送回站点，站点确认入库后重新公开待认领。");
        } else if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())) {
            String fault = dispute.getHandleResult();
            String faultLine;
            if (FAULT_CLAIMER.equals(fault)) {
                faultLine = "定责：认领人（取件后使用或搬运不当），取件奖励 +1 已回滚并计一次误领。";
            } else if (FAULT_PUBLISHER.equals(fault)) {
                faultLine = "定责：发布人（隐瞒破损或描述严重失实），发布人被扣分，你的奖励保留。";
            } else {
                faultLine = "定责：站点（保管或流转环节损坏），不扣任何用户的诚信分，已记入站点考核。";
            }
            sb.append("结论：物品损坏成立。处理意见：").append(handleNote).append("。").append(faultLine)
                    .append("该件由站点收回后作废归档、不再重新公开。请把实物送回站点，感谢你如实反馈。");
        } else {
            sb.append("结论：申诉成立。处理意见：").append(handleNote)
                    .append("。该次认领已作废、物品作废归档，本次取件奖励 +1 已回滚。");
        }

        messageService.push(dispute.getApplicantId(), title, sb.toString(),
                "DISPUTE", dispute.getItemId(), dispute.getApplyId());
    }

    /**
     * 定责完成后通知该物品所属投放点负责人：告诉站点结论是什么、线下接下来该做什么。
     * 站点不是定责人，但它是唯一能动手的人（收回实物、撤架归档、协助取件）。
     */
    private void notifyPointAdminResult(Dispute dispute, boolean approved) {
        if (dispute.getItemId() == null) {
            return;
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null || item.getDropPointId() == null) {
            return;
        }
        DropPoint point = dropPointMapper.selectById(item.getDropPointId());
        if (point == null || point.getAdminId() == null) {
            return;
        }

        String label = disputeTypeLabel(dispute.getDisputeType());
        String conclusion;
        String action;

        if (!approved) {
            conclusion = "申诉不成立（已驳回）";
            action = "实物与物品状态均未变化，无需额外线下操作；如认领人再来站点，按原流程正常交付即可。";
        } else if (TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())) {
            String result = dispute.getHandleResult();
            if (RESULT_FOUND.equals(result)) {
                conclusion = "核查结论：实物在站（误报）";
                action = "请协助认领人重新核对货架位置与存放区，确认后按正常流程签字取件。";
            } else if (RESULT_STOLEN.equals(result)) {
                conclusion = "核查结论：实物已被他人拿走";
                action = "物品已转「丢失待查」，认领资格保留。请把调取到的画面时间点、交接记录补进留档，便于后续追查或找回后重开。";
            } else {
                conclusion = "核查结论：从未实际投放（虚假招领）";
                action = "物品已作废、发布人积分已回滚并扣 2 分。请核对货架与库存确认无该实物后归档，无需再等失主取件。";
            }
        } else {
            int status = item.getItemStatus() == null ? -1 : item.getItemStatus();
            if (status == ItemStatus.RETURNING.getCode()) {
                conclusion = "认领已作废，物品判为「待归还」";
                action = "请等实物回到站点后点「确认归还入库」，物品将重新公开待认领；损坏情形按作废归档处理，不再回公开池。";
                if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())) {
                    String damageFault = dispute.getHandleResult();
                    if (FAULT_CLAIMER.equals(damageFault)) {
                        action = action + " 本单损坏定责为认领人责任（取件后使用或搬运不当），其取件奖励已回滚并计一次误领，站点不担责。";
                    } else if (FAULT_PUBLISHER.equals(damageFault)) {
                        action = action + " 本单损坏定责为发布人责任（隐瞒破损或描述失实），发布人已被扣 2 分，站点不担责。";
                    } else {
                        action = action + " 本单损坏定责为站点保管责任，已计入站点考核；双方用户诚信分均未变动，请核对存放位置与巡查记录。";
                    }
                }
            } else if (status == ItemStatus.VOIDED.getCode()) {
                conclusion = "认领已作废，物品判为「已作废」";
                action = "请把该物品从在站货架撤下并归档，不再对外展示。";
            } else if (status == ItemStatus.LOST.getCode()) {
                conclusion = "物品判为「丢失待查」";
                action = "请补充交接与调取记录，并留意是否有人送回。";
            } else {
                conclusion = "工单已通过";
                action = "请按结论核对实物状态，如需收回或归档请在在站清单处理。";
            }
        }

        String slaLine = "";
        if (dispute.getSiteUnresponsive() != null && dispute.getSiteUnresponsive() == 1) {
            LocalDateTime markedAt = dispute.getSiteUnresponsiveAt() != null
                    ? dispute.getSiteUnresponsiveAt() : dispute.getHandledAt();
            long waited = (dispute.getCreatedAt() == null || markedAt == null) ? 0
                    : Duration.between(dispute.getCreatedAt(), markedAt).toHours();
            slaLine = "本单站点超 " + SLA_POINT_CHECK_HOURS + " 小时未回填现场核验，已在 "
                    + formatTime(markedAt) + " 标记「站点未响应」（工单已等待 " + waited
                    + " 小时），该标记计入站点响应记录。";
        }

        String content = String.format(
                "工单#%d（%s·物品《%s》）已由系统管理员处理：%s。%s 处理意见：%s。"
                        + "你无需再回填现场核验，后续可在「纠纷核验」查看该工单结论。%s",
                dispute.getId(), label, item.getTitle(), conclusion, action,
                dispute.getHandlerNote() == null ? "无" : dispute.getHandlerNote(), slaLine);

        messageService.push(point.getAdminId(), "工单结论 · " + label + " → " + conclusion, content,
                "DISPUTE", item.getId(), dispute.getApplyId());
    }

    /**
     * 冒领申诉通过：回滚发布人+3/+1、认领人+1，认定冒领人恶意并扣 5 分、计一次误领，
     * 物品转为「待归还」，由投放点确认实物收回后再重新公开待认领。
     */
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

        String label = disputeTypeLabel(dispute.getDisputeType());
        String reason = label + "（工单#" + dispute.getId() + "）：" + request.getHandlerNote();

        // 冒领人这次成交无效：他的 +1 回滚，转发带来的 +1 也一并撤回；
        // 发布人的拾获 +3 随这次无效成交回滚；投放/交物的 +1 保留（实物确实交到了站点，图2 规则：保留失物投放的 1 分）。
        // 实物收回重新公开后，真失主再次取件会重新结算拾获奖励。
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
        rollbackForwardReward(item, apply, reason, operatorId);

        // 恶意冒领：扣诚信分并计次（管理员不参与积分体系，由 issueCredit 内部跳过）
        creditService.issueCreditOnce(apply.getClaimerId(), -FALSE_CLAIM_PENALTY, "FALSE_CLAIM_PENALTY",
                item.getId(), apply.getId(), label + "：恶意冒领扣分（工单#" + dispute.getId() + "）", operatorId);
        bumpMisclaimCount(apply.getClaimerId(), apply.getId());

        apply.setCreditRollback(1);
        claimApplyMapper.updateById(apply);

        item.setItemStatus(ItemStatus.RETURNING.getCode());
        foundItemMapper.updateById(item);

        notifyPublisherInvalidated(item, dispute.getId(), label,
                "该认领已被判定为恶意冒领，原认领作废。",
                "冒领人已被扣 5 分并计一次误领；该次拾获奖励已回滚，实物收回重新公开后由真失主重新结算。", true, true);
        notifyClaimerInvalidated(apply, item, dispute.getId(), label,
                "你因此前领取的物品并不属于你（冒领），本次认领作废。",
                "你被扣除诚信分 5 分，并计一次误领。",
                "本次取件奖励 +1 分已回滚。", true);
    }

    /**
     * 纠纷申诉通过：
     * 「物品不符」「物品损坏」实物都会被站点收回（转「待归还」），属「失物在站」，
     * 发布人正常投放与交物都是真实行为，拾获 +3 与交物 +1 一律不回滚；
     * 损坏按定责方区分：站点保管担责 / 认领人使用不当 / 发布人隐瞒破损；
     * 「其他纠纷」判作废，物品离开可控范围，发布人奖励才回滚。
     * 认领人的取件 +1 只在「这次成交确实不该由他拿」时回滚（不符、认领人担责的损坏、其他）。
     */
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

        String label = disputeTypeLabel(dispute.getDisputeType());
        String reason = label + "（工单#" + dispute.getId() + "）：" + request.getHandlerNote();

        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        boolean typeMismatch = TYPE_ITEM_MISMATCH.equals(dispute.getDisputeType());
        boolean typeDamaged = TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType());
        // 损坏件实物收回后按定责作废，拾获奖励保留（物品已毁，不会再产生新的有效成交）；
        // 物品不符属于「领错」：这次成交无效，拾获奖励随认领作废回滚，实物重新公开由真失主认领时重新结算
        boolean publisherCreditsStay = typeDamaged;

        if (!publisherCreditsStay) {
            // 发放已按「成交」绑定 applyId，这里按同一笔申请回滚，保证「领错→退回→真失主再取」可以再结算
            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
            // 投放/交物的 +1 按图2 只在不符以外的场景回滚：不符保留（东西确实交到了站点，只是这次成交无效）
            if (!typeMismatch) {
                rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
            }
        }
        // 这次成交被判定无效，转发带来的 +1 撤回；但损坏三行都写「转发不动」，不追溯损坏件的转发奖励
        if (!typeDamaged) {
            rollbackForwardReward(item, apply, reason, operatorId);
        }

        apply.setCreditRollback(1);
        claimApplyMapper.updateById(apply);

        if (typeMismatch) {
            // 误领：认领人拿了不属于自己的成交奖励 → 回滚 +1 并计一次误领，但不扣分
            rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
            bumpMisclaimCount(apply.getClaimerId(), apply.getId());
            item.setItemStatus(ItemStatus.RETURNING.getCode());
            foundItemMapper.updateById(item);
            notifyPublisherInvalidated(item, dispute.getId(), label,
                    "认领人发现领取的物品与描述不符，该次认领已作废。",
                    "误领人不扣分，仅计一次误领。", true, true);
            notifyClaimerInvalidated(apply, item, dispute.getId(), label,
                    "你领取的物品与招领描述不符（误领），本次认领已作废。",
                    "本次误领计 1 次，不扣诚信分。",
                    "本次取件奖励 +1 分已回滚。", true);
        } else if (typeDamaged) {
            String fault = request.getOutcome();
            if (fault == null || fault.isBlank()) {
                fault = FAULT_SITE;
            }
            if (!FAULT_SITE.equals(fault) && !FAULT_CLAIMER.equals(fault) && !FAULT_PUBLISHER.equals(fault)) {
                throw new BusinessException(400, "损坏定责方只能是站点 / 认领人 / 发布人三种之一");
            }
            dispute.setHandleResult(fault);

            String faultText;
            String claimerCreditLine;
            if (FAULT_CLAIMER.equals(fault)) {
                // 认领人担责：回滚他的 +1 并计一次误领，发布人与站点不动
                rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
                bumpMisclaimCount(apply.getClaimerId(), apply.getId());
                faultText = "经核验，损坏发生在取件之后（使用或搬运不当），由认领人担责。";
                claimerCreditLine = "本次损坏定责为你的责任，取件奖励 +1 分已回滚，并计一次误领。";
            } else if (FAULT_PUBLISHER.equals(fault)) {
                // 发布人担责：物品交付时就有未披露的破损，扣分；认领人不动
                creditService.issueCreditOnce(publisherId, -FALSE_PUBLISH_PENALTY, "FALSE_PUBLISH_PENALTY",
                        item.getId(), apply.getId(),
                        "损坏件：发布时隐瞒破损或描述严重失实，由发布人担责并扣 2 分（工单#" + dispute.getId() + "）",
                        operatorId);
                faultText = "经核验，物品在交付前已存在未披露的破损，由发布人担责（记扣 2 分）。";
                claimerCreditLine = "本次损坏定责为发布人责任，你的取件奖励 +1 分不予回滚。";
            } else {
                // 站点担责：不扣任何用户的分，只记站点考核
                faultText = "经核验，损坏发生在站点保管或流转环节，由站点担责并记入站点考核，不与用户诚信分挂钩。";
                claimerCreditLine = "本次损坏定责为站点保管责任，你的取件奖励 +1 分不予回滚。";
            }
            // 损坏件按规则表「转发不动」，这里不撤回转发奖励
            item.setItemStatus(ItemStatus.RETURNING.getCode());
            foundItemMapper.updateById(item);
            notifyPublisherInvalidated(item, dispute.getId(), label,
                    "认领人反馈领取后物品已损坏，该次认领已作废，物品退回站点核验。" + faultText,
                    "该情形不计误领次数（除认定认领人担责外）。", false, false);
            notifyClaimerInvalidated(apply, item, dispute.getId(), label,
                    "你反馈的「物品损坏」已核实，本次认领已作废、物品退回站点。",
                    faultText, claimerCreditLine, false);
        } else {
            // 其他纠纷：判作废，物品不再公开，发布人奖励随之回滚
            rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
            rollbackForwardReward(item, apply, reason, operatorId);
            item.setItemStatus(ItemStatus.VOIDED.getCode());
            foundItemMapper.updateById(item);
            messageService.push(publisherId, "认领作废 · " + label,
                    "你发布的招领「" + item.getTitle() + "」最近一次认领（工单#" + dispute.getId()
                            + "）经审核不成立，该次认领已作废、物品已作废归档；该次发放的拾获奖励已全部回滚。物品归属仍为你。",
                    "DISPUTE", item.getId(), apply.getId());
            messageService.push(apply.getClaimerId(), "认领作废 · " + label,
                    "你认领的物品「" + item.getTitle() + "」申诉（工单#" + dispute.getId()
                            + "）已通过，本次认领作废、物品已作废归档；本次取件奖励 +1 分已回滚。",
                    "DISPUTE", item.getId(), apply.getId());
        }
    }

    /**
     * 找不到物品核查通过：按站点核查结论分三种处理。
     * FOUND         实物在站，属误报 —— 不改任何状态，留档；
     * NOT_DELIVERED 从未实际投放（虚假招领）—— 物品作废、回滚发布人拾获加分并扣 2 分、本单终止、解锁其他锁定申请；
     * STOLEN        实物被他人拿走 —— 物品转「丢失待查」，发布人无责，认领人保留取件资格。
     */
    private void handleItemNotFound(Dispute dispute, DisputeHandleRequest request, Long operatorId) {
        String outcome = request.getOutcome();
        if (outcome == null || outcome.isBlank()) {
            throw new BusinessException(400, "请选择核查结论");
        }
        if (dispute.getApplyId() == null) {
            throw new BusinessException(400, "该工单缺少关联认领申请");
        }
        ClaimApply apply = claimApplyMapper.selectById(dispute.getApplyId());
        if (apply == null) {
            throw new BusinessException(404, "关联认领申请不存在");
        }
        FoundItem item = foundItemMapper.selectById(dispute.getItemId());
        if (item == null) {
            throw new BusinessException(404, "关联物品不存在");
        }

        dispute.setHandleResult(outcome);

        if (RESULT_FOUND.equals(outcome)) {
            // 实物在站：误报，不动状态与积分，认领人继续按正常流程取件
            messageService.push(apply.getClaimerId(), "取件核查 · 实物在站",
                    "你申报的「找不到物品」（工单#" + dispute.getId() + "）经站点核查：物品「" + item.getTitle()
                            + "」确实在站，可能是货架位置或时段差异。你的认领资格与诚信分均未受影响，请回到站点重新核对后上传签字合影确认领取。",
                    "DISPUTE", item.getId(), apply.getId());
            log.info("找不到物品核查：实物在站，误报。disputeId={}, itemId={}", dispute.getId(), item.getId());
            return;
        }

        if (RESULT_STOLEN.equals(outcome)) {
            // 被他人拿走：发布人已正常投放，无责；物品挂起待查，认领人保留资格
            item.setItemStatus(ItemStatus.LOST.getCode());
            foundItemMapper.updateById(item);
            messageService.push(apply.getClaimerId(), "取件核查 · 实物已被他人拿走",
                    "你申报的「找不到物品」（工单#" + dispute.getId() + "）经核查：物品「" + item.getTitle()
                            + "」在站点已被他人拿走。物品已转「丢失待查」，发布人不担责，你的取件资格予以保留，请等待站点进一步处理。",
                    "DISPUTE", item.getId(), apply.getId());
            log.info("找不到物品核查：实物被他人拿走，物品转丢失待查。disputeId={}, itemId={}",
                    dispute.getId(), item.getId());
            return;
        }

        if (!RESULT_NOT_DELIVERED.equals(outcome)) {
            throw new BusinessException(400, "无效的核查结论");
        }

        // 从未实际投放：虚假招领，回滚发布人拾获加分并处罚
        String reason = "虚假招领（未实际投放实物，工单#" + dispute.getId() + "）：" + request.getHandlerNote();
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        // 发放按成交绑定了 applyId，这里按同一笔申请回滚
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        // 投放/交物的 +1 保留：图2 该行只要求回滚拾获的 3 分并扣 2 分
        rollbackForwardReward(item, apply, reason, operatorId);
        creditService.issueCreditOnce(publisherId, -FALSE_PUBLISH_PENALTY, "FALSE_PUBLISH_PENALTY",
                item.getId(), apply.getId(), "虚假招领（物品不见）：未实际投放实物，扣 2 分（工单#" + dispute.getId() + "）", operatorId);

        item.setItemStatus(ItemStatus.VOIDED.getCode());
        foundItemMapper.updateById(item);

        apply.setApplyStatus(ClaimStatus.REJECTED.getCode());
        apply.setRejectReason("站点核查：投放点无该实物，招领已作废");
        apply.setCreditRollback(1);
        claimApplyMapper.updateById(apply);

        // 物品已作废，同物品其他排队锁定的申请一并作废：实物都不存在了，推回「待审核」也没东西可交付
        List<ClaimApply> otherLocked = claimApplyMapper.selectList(new LambdaQueryWrapper<ClaimApply>()
                .eq(ClaimApply::getItemId, item.getId())
                .ne(ClaimApply::getId, apply.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode()));
        if (!otherLocked.isEmpty()) {
            claimApplyMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ClaimApply>()
                    .set(ClaimApply::getApplyStatus, ClaimStatus.REJECTED.getCode())
                    .set(ClaimApply::getRejectReason, "站点核查：投放点无该实物，招领已作废，本次申请一并关闭")
                    .eq(ClaimApply::getItemId, item.getId())
                    .ne(ClaimApply::getId, apply.getId())
                    .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode()));
            for (ClaimApply other : otherLocked) {
                messageService.push(other.getClaimerId(), "认领申请已关闭 · 招领作废",
                        "你申请认领的物品「" + item.getTitle() + "」经站点核查：投放点没有该实物，招领已作废。"
                                + "你此前的排队申请已一并关闭（不记驳回次数、不影响诚信分）。",
                        "DISPUTE", item.getId(), other.getId());
            }
        }

        log.info("找不到物品核查：未实际投放，物品作废并回滚发布人积分。disputeId={}, itemId={}, publisher={}",
                dispute.getId(), item.getId(), publisherId);

        messageService.push(publisherId, "招领作废 · 未实际投放实物",
                "你发布的招领「" + item.getTitle() + "」经站点核查（工单#" + dispute.getId()
                        + "）：投放点并无该实物（虚假招领）。招领已作废，此前发放的拾获奖励（+3 / +1）已全部回滚，并扣诚信分 2 分。",
                "DISPUTE", item.getId(), apply.getId());
        messageService.push(apply.getClaimerId(), "认领终止 · 招领已作废",
                "你申请的物品「" + item.getTitle() + "」经站点核查（工单#" + dispute.getId()
                        + "）未实际投放实物，该招领已作废，本单终止。你无责、不扣分，可重新浏览其他失物。",
                "DISPUTE", item.getId(), apply.getId());
    }

    private String disputeTypeLabel(String type) {
        if (TYPE_ITEM_MISMATCH.equals(type)) {
            return "物品不符";
        }
        if (TYPE_ITEM_DAMAGED.equals(type)) {
            return "物品损坏（失物损失）";
        }
        if (TYPE_FALSE_CLAIM.equals(type)) {
            return "物品被冒领（失物不见）";
        }
        if (TYPE_ITEM_NOT_FOUND.equals(type)) {
            return "找不到物品";
        }
        return "其他纠纷";
    }

    private String dropPointNameOf(FoundItem item) {
        if (item.getDropPointId() == null) {
            return "原投放点";
        }
        DropPoint dp = dropPointMapper.selectById(item.getDropPointId());
        return (dp == null || dp.getName() == null) ? "原投放点" : dp.getName();
    }

    /**
     * 认领作废时通知原招领者：物品归属不变、奖励是否回滚；republish=false 表示损坏件作废、不再回公开池
     * creditsRolledBack=false 表示本次作废不回收他的拾获 +3 与交物 +1（失物在站 / 发布人无过错）
     */
    private void notifyPublisherInvalidated(FoundItem item, Long disputeId, String label,
                                            String verdict, String extra, boolean republish,
                                            boolean creditsRolledBack) {
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        StringBuilder sb = new StringBuilder();
        sb.append("你发布的招领「").append(item.getTitle()).append("」最近一次认领（工单#").append(disputeId)
                .append("）经审核认定：").append(label).append("。").append(verdict).append(" ");
        if (extra != null && !extra.isBlank()) {
            sb.append(extra).append(" ");
        }
        if (republish) {
            if (creditsRolledBack) {
                sb.append("该次发放的拾获奖励（+3 / +1）已随认领作废一并回滚；物品已转为「待归还」，站点收回实物后将重新公开，")
                        .append("真失主再次认领并取件时会重新结算拾获奖励。物品归属仍为你，不会转给误领人。");
            } else {
                sb.append("该次发放的拾获奖励（拾获 +3、交物 +1）不回收：实物仍在站点手中、会收回后重新公开，")
                        .append("同一件物品的拾获奖励按物品只结算一次，不会再重复发放。物品归属仍为你，不会转给误领人。");
            }
        } else {
            sb.append("该件已定责为「物品损坏」，站点收回后将作废归档，不会再进入公开待认领池。");
            if (creditsRolledBack) {
                sb.append("该次发放的拾获奖励已随认领作废一并回滚。");
            } else {
                sb.append("本次作废不回收你的拾获与交物奖励（非你过错），奖励按物品只结算一次。");
            }
            sb.append("物品归属仍为你，不会转给误领人。");
        }
        messageService.push(publisherId, "认领作废 · " + label, sb.toString(), "DISPUTE", item.getId(), null);
    }

    /** 认领作废时通知误领人：奖励已回滚、需要把实物送回站点；损坏件送达后作废，不再重新公开 */
    private void notifyClaimerInvalidated(ClaimApply apply, FoundItem item, Long disputeId, String label,
                                          String verdict, String extra, String creditLine, boolean republish) {
        StringBuilder sb = new StringBuilder();
        sb.append("你认领的物品「").append(item.getTitle()).append("」经审核认定：").append(label).append("。")
                .append(verdict).append(" ");
        if (extra != null && !extra.isBlank()) {
            sb.append(extra).append(" ");
        }
        if (creditLine != null && !creditLine.isBlank()) {
            sb.append(creditLine).append(" ");
        }
        if (republish) {
            sb.append("请将实物送回「").append(dropPointNameOf(item))
                    .append("」，站点确认归还入库后，物品会重新公开待认领。");
        } else {
            sb.append("请将实物送回「").append(dropPointNameOf(item))
                    .append("」，站点确认收回后该件将作废归档，不再对外开放认领。");
        }
        messageService.push(apply.getClaimerId(), "认领作废 · " + label, sb.toString(), "DISPUTE", item.getId(), apply.getId());
    }

    /**
     * 误领/冒领计次：非恶意误领只记账不扣分。
     * 幂等按「认领申请」：同一笔认领不论几张工单通过只计一次——用一条 change_amount=0 的
     * MISCLAIM_COUNT 流水作为「该笔已计次」的数据库凭证（不影响积分，只承担事实记录）。
     */
    private void bumpMisclaimCount(Long userId, Long applyId) {
        if (userId == null) {
            return;
        }
        if (applyId != null) {
            LambdaQueryWrapper<CreditLog> counted = new LambdaQueryWrapper<>();
            counted.eq(CreditLog::getUserId, userId)
                    .eq(CreditLog::getOperationType, "MISCLAIM_COUNT")
                    .eq(CreditLog::getRelatedApplyId, applyId);
            if (creditLogMapper.selectCount(counted) > 0) {
                log.info("该笔认领已计过误领次数，跳过：user={}, applyId={}", userId, applyId);
                return;
            }
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int current = user.getMisclaimCount() != null ? user.getMisclaimCount() : 0;
        user.setMisclaimCount(current + 1);
        userMapper.updateById(user);

        if (applyId != null) {
            CreditLog marker = new CreditLog();
            marker.setUserId(userId);
            marker.setChangeAmount(0);
            marker.setOperationType("MISCLAIM_COUNT");
            marker.setRelatedApplyId(applyId);
            marker.setReason("误领计次凭证（不影响积分）");
            creditLogMapper.insert(marker);
        }
    }

    private void rollbackSingle(Long userId, String operationType, int amount,
                                Long itemId, Long applyId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType)
                .eq(CreditLog::getRelatedItemId, itemId);
        // 按「认领申请」粒度定位发放流水：同一物品被重新认领属于新的一笔，两笔各自都要回滚
        if (applyId != null) {
            issuedWrapper.eq(CreditLog::getRelatedApplyId, applyId);
        }
        if (creditLogMapper.selectCount(issuedWrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -amount);
        if (applyId != null) {
            rollbackWrapper.eq(CreditLog::getRelatedApplyId, applyId);
        }
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(userId, -amount, "ROLLBACK", itemId, applyId, reason, operatorId);
    }

    /**
     * 转发奖励 +1 随认领作废一并回滚，堵住「小号转发自己的招领 → 认领 → 申诉通过」的刷分通道。
     * 只在「这笔申请确实发过 FORWARD_REWARD」时扣 1 分，且判断条件与发放时一致：
     * 转发人不是发布人、也不是该笔认领人，避免给没有奖励的人凭空扣分。
     */
    private void rollbackForwardReward(FoundItem item, ClaimApply apply, String reason, Long operatorId) {
        if (item == null || apply == null || item.getForwarderId() == null) {
            return;
        }
        Long forwarderId = item.getForwarderId();
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        if (forwarderId.equals(publisherId) || forwarderId.equals(apply.getClaimerId())) {
            return;
        }

        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, forwarderId)
                .eq(CreditLog::getOperationType, "FORWARD_REWARD")
                .eq(CreditLog::getRelatedItemId, item.getId());
        if (apply.getId() != null) {
            issuedWrapper.eq(CreditLog::getRelatedApplyId, apply.getId());
        }
        if (creditLogMapper.selectCount(issuedWrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, forwarderId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, item.getId())
                .eq(CreditLog::getChangeAmount, -1);
        if (apply.getId() != null) {
            rollbackWrapper.eq(CreditLog::getRelatedApplyId, apply.getId());
        }
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(forwarderId, -1, "ROLLBACK", item.getId(), apply.getId(),
                "招领转发奖励回滚：" + reason, operatorId);
        log.info("转发奖励已回滚：forwarder={}, itemId={}, applyId={}", forwarderId, item.getId(), apply.getId());
    }

    private Dispute getDisputeOrThrow(Long id) {
        Dispute dispute = disputeMapper.selectById(id);
        if (dispute == null) {
            throw new BusinessException(404, "申诉工单不存在");
        }
        return dispute;
    }

    /**
     * 站点回填 SLA 催办：待处理且站点未回填的工单，满 24h 就给系统管理员生成催办任务。
     * 同一工单每满 24h 催一次、最多 3 次；已催过的 24h 内不重复催，站点回填后自然停止。
     */
    @Override
    @Transactional
    public int escalateUnresponsiveSites() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaCutoff = now.minusHours(SLA_POINT_CHECK_HOURS);
        LocalDateTime remindCutoff = now.minusHours(SLA_POINT_CHECK_HOURS);

        List<Dispute> candidates = disputeMapper.selectList(new LambdaQueryWrapper<Dispute>()
                .eq(Dispute::getStatus, STATUS_PENDING)
                .le(Dispute::getCreatedAt, slaCutoff)
                .orderByAsc(Dispute::getCreatedAt));

        int count = 0;
        for (Dispute d : candidates) {
            // 站点已回填（含管理员代填）就不催
            if (StringUtils.hasText(d.getPointCheckNote())) {
                continue;
            }
            int reminded = d.getPointRemindCount() == null ? 0 : d.getPointRemindCount();
            if (reminded >= MAX_POINT_REMIND) {
                continue;
            }
            if (d.getPointEscalatedAt() != null && d.getPointEscalatedAt().isAfter(remindCutoff)) {
                continue;
            }

            d.setPointRemindCount(reminded + 1);
            d.setPointEscalatedAt(now);
            disputeMapper.updateById(d);
            notifySysAdminsSla(d, reminded + 1);
            count++;
        }

        if (count > 0) {
            log.warn("站点回填 SLA 催办完成：本次催办 {} 单（SLA={}h，单笔最多{}次）",
                    count, SLA_POINT_CHECK_HOURS, MAX_POINT_REMIND);
        }
        return count;
    }

    /** 催办通知发给全体系统管理员：带站点负责人与电话，便于直接打电话催，催不到就代填或定责 */
    private void notifySysAdminsSla(Dispute dispute, int remindNo) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.SYS_ADMIN));
        if (admins.isEmpty()) {
            log.warn("无系统管理员账号，SLA 催办无法投递：disputeId={}", dispute.getId());
            return;
        }

        FoundItem item = dispute.getItemId() == null ? null : foundItemMapper.selectById(dispute.getItemId());
        DropPoint point = (item != null && item.getDropPointId() != null)
                ? dropPointMapper.selectById(item.getDropPointId()) : null;
        User pointAdmin = (point != null && point.getAdminId() != null)
                ? userMapper.selectById(point.getAdminId()) : null;

        long waited = dispute.getCreatedAt() == null ? SLA_POINT_CHECK_HOURS
                : Duration.between(dispute.getCreatedAt(), LocalDateTime.now()).toHours();
        String pointName = point != null ? point.getName() : "未关联投放点";
        String pointOwner = pointAdmin == null ? "未分配负责人"
                : displayName(pointAdmin) + (StringUtils.hasText(pointAdmin.getPhone())
                ? "（" + pointAdmin.getPhone() + "）" : "（未登记电话）");

        String content = String.format(
                "工单#%d（%s·物品《%s》）已等待 %d 小时，站点「%s」负责人 %s 仍未回填现场核验（第 %d/%d 次催办）。"
                        + "请先电话联系站点核实实物现状；联系不上可由你电话核实后代填，或直接定责——"
                        + "定责时系统会自动把本单标记为「站点未响应」并记录标记时间。",
                dispute.getId(), disputeTypeLabel(dispute.getDisputeType()),
                item == null ? "未关联物品" : item.getTitle(), waited,
                pointName, pointOwner, remindNo, MAX_POINT_REMIND);

        String title = "催办 · 站点超 " + SLA_POINT_CHECK_HOURS + "h 未回填（工单#" + dispute.getId() + "）";
        for (User admin : admins) {
            messageService.push(admin.getId(), title, content,
                    "DISPUTE_SLA", dispute.getItemId(), dispute.getApplyId());
        }
        log.info("SLA 催办已投递：disputeId={}, 第{}次, 管理员数={}", dispute.getId(), remindNo, admins.size());
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : time.toString().replace('T', ' ').substring(0, 16);
    }

    private DisputeVO toVO(Dispute d, String itemTitle) {
        return DisputeVO.fromEntity(d, null, itemTitle);
    }
}
