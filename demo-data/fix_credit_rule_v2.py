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

# 1) 冒领：只回滚拾获 +3，保留投放/交物 +1
rep('''        // 发布人的拾获 +3 / 交物 +1 同属这次无效成交，一并回滚，实物收回重新公开后由真失主重新结算。
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);''',
'''        // 发布人的拾获 +3 随这次无效成交回滚；投放/交物的 +1 保留（实物确实交到了站点，图2 规则：保留失物投放的 1 分）。
        // 实物收回重新公开后，真失主再次取件会重新结算拾获奖励。
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);''',
'false-claim-check')

# 2) 不符：保留投放 +1；其他纠纷才回滚
rep('''            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
            // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）
            rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
        }''',
'''            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
            // 投放/交物的 +1 按图2 只在不符以外的场景回滚：不符保留（东西确实交到了站点，只是这次成交无效）
            if (!typeMismatch) {
                rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
            }
        }''',
'mismatch-check')

# 3) 从未投放：只回滚拾获 3 分，保留投放 1 分
rep('''        // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
        rollbackForwardReward(item, apply, reason, operatorId);
        creditService.issueCreditOnce(publisherId, -FALSE_PUBLISH_PENALTY, "FALSE_PUBLISH_PENALTY",''',
'''        // 投放/交物的 +1 保留：图2 该行只要求回滚拾获的 3 分并扣 2 分
        rollbackForwardReward(item, apply, reason, operatorId);
        creditService.issueCreditOnce(publisherId, -FALSE_PUBLISH_PENALTY, "FALSE_PUBLISH_PENALTY",''',
'not-delivered-check')

# 4) 从未投放：其他排队锁定的申请作废（物品已作废，推回待审核无法交付）
rep('''        // 同物品其他被锁定的申请解锁回待审核
        claimApplyMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ClaimApply>()
                .set(ClaimApply::getApplyStatus, ClaimStatus.PENDING.getCode())
                .eq(ClaimApply::getItemId, item.getId())
                .ne(ClaimApply::getId, apply.getId())
                .eq(ClaimApply::getApplyStatus, ClaimStatus.LOCKED.getCode()));''',
'''        // 物品已作废，同物品其他排队锁定的申请一并作废：实物都不存在了，推回「待审核」也没东西可交付
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
        }''',
'not-delivered-void-locks')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
