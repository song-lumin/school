# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\ReportServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

# 1) 举报成立（虚假招领）时补回滚转发 +1
old1 = '''        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), reason, operatorId);

        item.setItemStatus(ItemStatus.VOIDED.getCode());'''
new1 = '''        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), reason, operatorId);
        // 举报成立说明该笔招领整体作废，转发带来的 +1 一并撤回
        rollbackForwardReward(item, reason, operatorId);

        item.setItemStatus(ItemStatus.VOIDED.getCode());'''
print('call match:', s.count(old1))
if s.count(old1) == 1:
    s = s.replace(old1, new1, 1)

# 2) 插入私有方法
anchor = '''    private void rollbackSingle(Long userId, String operationType, int amount,'''
method = '''    /**
     * 举报成立、整条招领作废时，把转发带来的 +1 撤回；只在该物品上确实发过 FORWARD_REWARD 时扣分。
     */
    private void rollbackForwardReward(FoundItem item, String reason, Long operatorId) {
        if (item == null || item.getForwarderId() == null) {
            return;
        }
        Long forwarderId = item.getForwarderId();
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        if (forwarderId.equals(publisherId)) {
            return;
        }

        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, forwarderId)
                .eq(CreditLog::getOperationType, "FORWARD_REWARD")
                .eq(CreditLog::getRelatedItemId, item.getId());
        if (creditLogMapper.selectCount(issuedWrapper) == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, forwarderId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, item.getId())
                .eq(CreditLog::getChangeAmount, -1);
        if (creditLogMapper.selectCount(rollbackWrapper) > 0) {
            return;
        }

        creditService.issueCredit(forwarderId, -1, "ROLLBACK", item.getId(), null,
                "举报成立 · 转发奖励回滚：" + reason, operatorId);
    }

'''
print('anchor match:', s.count(anchor))
if s.count(anchor) == 1:
    s = s.replace(anchor, method + anchor, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('done')
