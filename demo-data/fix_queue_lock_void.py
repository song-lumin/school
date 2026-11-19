# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\ClaimServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c == 1:
        s = s.replace(old, new, 1)
    else:
        raise SystemExit('ABORT ' + tag)

# ---------- 1) 现场取消：排队锁一律作废，不再回待审核 ----------
old1 = '''        // 只解锁「因他人被同意而临时锁定」的申请：答错 3 次进入惩罚期的申请不能随一次取消一起放出，
        // 否则违规用户跳过 24 小时锁定，且 reject_count 仍留着 3，规则形同虚设
        LambdaQueryWrapper<ClaimApply> lockedWrapper = new LambdaQueryWrapper<>();
        lockedWrapper.eq(ClaimApply::getItemId, item.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode());
        List<ClaimApply> lockedList = claimApplyMapper.selectList(lockedWrapper);

        List<ClaimApply> reopenedApplies = new java.util.ArrayList<>();
        List<ClaimApply> stillLocked = new java.util.ArrayList<>();
        List<Long> reopenIds = new java.util.ArrayList<>();
        for (ClaimApply lockedApply : lockedList) {
            int rejectCount = lockedApply.getRejectCount() == null ? 0 : lockedApply.getRejectCount();
            if (rejectCount >= MAX_REJECT_COUNT) {
                stillLocked.add(lockedApply);
            } else {
                reopenedApplies.add(lockedApply);
                reopenIds.add(lockedApply.getId());
            }
        }
        if (!reopenIds.isEmpty()) {
            LambdaQueryWrapper<ClaimApply> reopenWrapper = new LambdaQueryWrapper<>();
            reopenWrapper.in(ClaimApply::getId, reopenIds);
            ClaimApply lockedUpdate = new ClaimApply();
            lockedUpdate.setApplyStatus(ClaimStatus.PENDING.getCode());
            claimApplyMapper.update(lockedUpdate, reopenWrapper);
        }

        long pendingCount = reopenedApplies.size();
        if (pendingCount == 0) {
            // 无其他可恢复的申请，物品退回公开待认领
            item.setItemStatus(ItemStatus.PUBLIC.getCode());
        }
        // 还有其他待审核回复则保持认领中(2)
        foundItemMapper.updateById(item);'''

new1 = '''        // 排队锁（因他人被审核通过而临时锁定）一律作废：与「取件成功后其余锁定单全部作废」同一结论。
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
        foundItemMapper.updateById(item);'''
rep(old1, new1, 'cancel-lock-block')

old2 = '''        String stateForPublisher = pendingCount == 0
                ? "实物未离站，仍在「" + pointName + "」，该招领已重新回到「公开待认领」，有新申请时你会收到审核提醒。"
                : "实物未离站，仍在「" + pointName + "」，同物品还有 " + pendingCount
                        + " 条申请已恢复为「待审核」，请到「认领审核」里重新处理。";
        String stateForPoint = pendingCount == 0
                ? "该招领已重新回到「公开待认领」，可继续接收新申请。"
                : "同物品还有 " + pendingCount + " 条申请已恢复为「待审核」，等待招领人或站点重新处理。";'''
new2 = '''        String stateForPublisher = "实物未离站，仍在「" + pointName + "」，该招领已重新回到「公开待认领」；"
                + "同物品其余排队申请已一并作废，有新申请时你会收到审核提醒。";
        String stateForPoint = "该招领已重新回到「公开待认领」，同物品其余排队申请已作废，可继续接收新申请。";'''
rep(old2, new2, 'cancel-text')

old3 = '''        if (!reopenIds.isEmpty()) {
            // no-op
        }'''
# 跳过（不存在）

old4 = '''        // 被解锁的申请人也要知道自己的申请重新进入审核，不必再重新提交
        for (ClaimApply reopened : reopenedApplies) {
            messageService.push(reopened.getClaimerId(), "你的认领申请已恢复待审核",
                    "物品「" + item.getTitle() + "」此前由他人取件时现场取消，你提交的认领申请已恢复为「待审核」，"
                            + "无需重新提交，等待招领人或站点管理员处理即可。",
                    "CLAIM", item.getId(), reopened.getId());
        }'''
new4 = '''        // 被作废的排队申请人要知道：这不算他答错，也不会记驳回次数，想要就重新提交
        for (ClaimApply voided : voidedApplies) {
            messageService.push(voided.getClaimerId(), "你的认领申请已作废 · 物品重新公开",
                    "物品「" + item.getTitle() + "」的原认领人现场退回，实物未离站，该物品已重新回到「公开待认领」。"
                            + "你此前的排队申请已作废（不记驳回次数、不影响诚信分），如需认领请重新提交。",
                    "CLAIM", item.getId(), voided.getId());
        }'''
rep(old4, new4, 'cancel-notify')

# ---------- 2) 每日清理：物品已回公开的排队锁同样作废 ----------
old5 = '''     * 2. 物品已回到「公开」且驳回不足 3 次 → 恢复「待审核」，并推送「无需重新提交」；'''
new5 = '''     * 2. 物品已回到「公开」且驳回不足 3 次 → 作废（与现场取消、取件成功的结论一致，如需认领重新提交）；'''
rep(old5, new5, 'cleanup-doc')

old6 = '''            // 情况 2：物品回到公开、且不是惩罚性锁定 → 恢复待审核（与现场取消的自动解锁一致）
            if (itemStatus == ItemStatus.PUBLIC.getCode() && rejectCount < MAX_REJECT_COUNT) {
                apply.setApplyStatus(ClaimStatus.PENDING.getCode());
                apply.setRejectReason(null);
                claimApplyMapper.updateById(apply);
                messageService.push(apply.getClaimerId(), "你的认领申请已恢复待审核",
                        itemTitle + "已重新回到「公开待认领」，你此前被暂停的申请已自动恢复为「待审核」，"
                                + "无需重新提交，等待招领人或站点管理员处理即可。",
                        "CLAIM", apply.getItemId(), apply.getId());
                handled++;
                continue;
            }'''
new6 = '''            // 情况 2：物品回到公开、且不是惩罚性锁定 → 作废（与现场取消、取件成功保持同一结论）
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
            }'''
rep(old6, new6, 'cleanup-branch')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
