# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new, 1)

# ---- 1) 站点核验：只落事实、不改状态不动分，统一转系统管理员 ----
old_point = '''        // 现场核对完可以直接出结论：本点位负责人（或代看的管理员）按规则表当场结案，
        // 状态与积分由系统自动处理，不必再等系统管理员二次定责。
        String verdict = request.getOutcome() == null ? null : request.getOutcome().trim().toUpperCase();
        boolean hasVerdict = verdict != null && !verdict.isBlank() && !"OTHER".equals(verdict);
        if (hasVerdict) {
            boolean siteCanRule = TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())
                    || TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType());
            if (!siteCanRule) {
                throw new BusinessException(400, "该类型工单由系统管理员定责，站点只回填事实（可留空结论）");
            }
            if (TYPE_ITEM_NOT_FOUND.equals(dispute.getDisputeType())
                    && !(RESULT_FOUND.equals(verdict) || RESULT_NOT_DELIVERED.equals(verdict)
                    || RESULT_STOLEN.equals(verdict))) {
                throw new BusinessException(400, "找不到物品的结论只能是：实物在站 / 从未投放 / 被他人拿走 / 其他");
            }
            if (TYPE_ITEM_DAMAGED.equals(dispute.getDisputeType())
                    && !(FAULT_SITE.equals(verdict) || FAULT_CLAIMER.equals(verdict)
                    || FAULT_PUBLISHER.equals(verdict))) {
                throw new BusinessException(400, "损坏定责只能是：站点 / 认领人 / 发布人 / 其他");
            }
            DisputeHandleRequest handleRequest = new DisputeHandleRequest();
            handleRequest.setApproved(Boolean.TRUE);
            handleRequest.setOutcome(verdict);
            handleRequest.setHandlerNote(StringUtils.hasText(dispute.getPointCheckNote())
                    ? dispute.getPointCheckNote() : "站点现场核验后按结论结案");
            // 复用管理员定责同一套逻辑：物品状态、积分回滚/发放、申请人通知一次到位
            handleInternal(id, handleRequest, operatorId, true);
            log.info("站点现场结论结案：disputeId={}, verdict={}, operator={}", id, verdict, operatorId);
            return;
        }
'''

new_point = '''        // 站点只负责核对、调监控、联系沟通：这里只落事实，不预设结论、不碰积分，
        // 由系统管理员在「纠纷管理」里勾选要回滚哪几笔已发放的分并填写原因。
'''

rep(old_point, new_point, 'pointcheck-body')

old_tail = '''        // 选了「其他」：现场定不了，明确转给系统管理员，别让工单在队列里干等
        if ("OTHER".equals(verdict)) {
            dispute.setStatus(STATUS_PROCESSING);
            disputeMapper.updateById(dispute);
            for (User admin : userMapper.selectList(null)) {
                if (admin.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN) {
                    messageService.push(admin.getId(), "站点无法定论，请接手定责（工单#" + dispute.getId() + "）",
                            "「" + point.getName() + "」已接手本工单（状态：处理中），结论选了「其他」：" + dispute.getPointCheckNote()
                                    + "。请到「纠纷管理」按规则表定责。",
                            "DISPUTE", dispute.getItemId(), dispute.getApplyId());
                }
            }
        }
'''

new_tail = '''        // 站点反馈到位即转「处理中」，并明确喊系统管理员来定责（积分由管理员勾选回滚）
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
'''

rep(old_tail, new_tail, 'pointcheck-tail')

# ---- 2) 手动定责分支 ----
rep('''        if (request.getApproved()) {
            dispute.setStatus(STATUS_APPROVED);
            if (TYPE_FALSE_CLAIM.equals(dispute.getDisputeType())) {''',
'''        // 管理员勾选了回滚项（哪怕是空数组）就走手动定责：不按纠纷类型预设，勾哪笔退哪笔
        boolean manual = request.getRollbackItems() != null;
        if (manual) {
            dispute.setStatus(request.getApproved() ? STATUS_APPROVED : STATUS_REJECTED);
            handleManualSettle(dispute, request, operatorId);
        } else if (request.getApproved()) {
            dispute.setStatus(STATUS_APPROVED);
            if (TYPE_FALSE_CLAIM.equals(dispute.getDisputeType())) {''', 'manual-branch')

# ---- 3) 新增 handleManualSettle ----
rep('''    /**
     * 通知申诉人定责结论：通过要说清物品/积分怎么处置、自己下一步做什么；''',
'''    /**
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
     * 通知申诉人定责结论：通过要说清物品/积分怎么处置、自己下一步做什么；''', 'manual-method')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('backend saved')
