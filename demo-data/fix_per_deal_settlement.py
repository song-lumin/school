# -*- coding: utf-8 -*-
import io, sys

def patch(path, pairs):
    s = io.open(path, encoding='utf-8').read()
    ok = True
    for old, new in pairs:
        c = s.count(old)
        print(f'  match={c} :: {" ".join(old.strip().split())[:60]}')
        if c != 1:
            ok = False
            continue
        s = s.replace(old, new, 1)
    if ok:
        io.open(path, 'w', encoding='utf-8', newline='').write(s)
    return ok

base = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound'

# 1) 接口新增「这件物品上是否还有未回滚的拾获奖励」
print('CreditService.java')
patch(base + r'\service\CreditService.java', [(
'''    void issueCreditOnce(Long userId, int amount, String operationType,
                         Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);
}''',
'''    void issueCreditOnce(Long userId, int amount, String operationType,
                         Long relatedItemId, Long relatedApplyId, String reason, Long operatorId);

    /**
     * 该用户在这件物品上是否还有一笔「未回滚」的拾获 +3。
     * 用于按「成交」结算：首次发、上次成交被判无效已回滚后，真失主再次取件时补发一次。
     */
    boolean hasLivePickupReward(Long userId, Long itemId);
}''')])

# 2) 实现
print('CreditServiceImpl.java')
patch(base + r'\service\impl\CreditServiceImpl.java', [(
'''    private final RedisTemplate<String, Object> redisTemplate;''',
'''    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean hasLivePickupReward(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            return false;
        }
        LambdaQueryWrapper<CreditLog> issuedWrapper = new LambdaQueryWrapper<>();
        issuedWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "PICKUP_ISSUE")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, 3);
        long issued = creditLogMapper.selectCount(issuedWrapper);

        LambdaQueryWrapper<CreditLog> rolledWrapper = new LambdaQueryWrapper<>();
        rolledWrapper.eq(CreditLog::getUserId, userId)
                .eq(CreditLog::getOperationType, "ROLLBACK")
                .eq(CreditLog::getRelatedItemId, itemId)
                .eq(CreditLog::getChangeAmount, -3);
        long rolled = creditLogMapper.selectCount(rolledWrapper);
        return issued - rolled > 0;
    }''')])

# 3) 取件发放：按成交结算
print('ClaimServiceImpl.java')
patch(base + r'\service\impl\ClaimServiceImpl.java', [(
'''        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        // 发布人拾获奖励按「物品」只结算一次（不按申请单）：
        // 物品被误领退回、重新公开、再由真失主认领，属于同一件东西，不重复发放；
        // 因此后续纠纷也不再回收这 3 分 —— 否则「误领→退回→重认领」会让发布人被反复扣分。
        creditService.issueCreditOnce(publisherId, PICKUP_ISSUE_CREDIT, "PICKUP_ISSUE",
                item.getId(), null, "失主取件完成（拾获者，按物品一次性结算）", currentUserId);''',
'''        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        // 发布人拾获奖励按「成交」结算，但同一件物品同时只允许存在一笔未回滚的 +3：
        // 首次成交正常发放；上一次成交被判无效（领错 / 冒领 / 其他）并已回滚后，
        // 真失主再次认领属于新的一次有效成交，重新结算 +3；未被回滚则不再重复发放。
        if (!creditService.hasLivePickupReward(publisherId, item.getId())) {
            creditService.issueCreditOnce(publisherId, PICKUP_ISSUE_CREDIT, "PICKUP_ISSUE",
                    item.getId(), apply.getId(), "失主取件完成（拾获者）", currentUserId);
        }''')])

print('done step1')
