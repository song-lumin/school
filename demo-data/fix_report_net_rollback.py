# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\ReportServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

old = '''    private void rollbackSingle(Long userId, String operationType, int amount,
                                Long itemId, String reason, Long operatorId) {
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

        creditService.issueCredit(userId, -amount, "ROLLBACK", itemId, null, reason, operatorId);
    }'''

new = '''    private void rollbackSingle(Long userId, String operationType, int amount,
                                Long itemId, String reason, Long operatorId) {
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, operationType)
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, amount);
        long issued = creditLogMapper.selectCount(issuedWrapper);
        if (issued == 0) {
            return;
        }

        LambdaQueryWrapper<CreditLog> rollbackWrapper = new LambdaQueryWrapper<>();
        rollbackWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -amount);
        long rolled = creditLogMapper.selectCount(rollbackWrapper);

        // 按净额回滚：同一物品可能发生过多次成交，只有未被回滚的那几笔才需要扣
        long net = issued - rolled;
        if (net <= 0) {
            return;
        }

        creditService.issueCredit(userId, (int) (-amount * net), "ROLLBACK", itemId, null,
                reason, operatorId);
    }'''

print('match:', s.count(old))
if s.count(old) == 1:
    s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('done')
