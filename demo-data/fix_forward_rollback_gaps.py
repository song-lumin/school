# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

# 1) 损坏分支：认领作废、物品退回后作废，转发 +1 应与不符/冒领一样撤回
old1 = '''            item.setItemStatus(ItemStatus.RETURNING.getCode());
            foundItemMapper.updateById(item);
            notifyPublisherInvalidated(item, dispute.getId(), label,
                    "认领人反馈领取后物品已损坏'''
new1 = '''            // 这次成交被判定无效，转发带来的 +1 一律撤回（与不符/冒领口径一致）
            rollbackForwardReward(item, apply, reason, operatorId);
            item.setItemStatus(ItemStatus.RETURNING.getCode());
            foundItemMapper.updateById(item);
            notifyPublisherInvalidated(item, dispute.getId(), label,
                    "认领人反馈领取后物品已损坏'''
print('damaged match:', s.count(old1))
if s.count(old1) == 1:
    s = s.replace(old1, new1, 1)

# 2) 其他纠纷：物品作废，转发 +1 同样撤回
old2 = '''            // 其他纠纷：判作废，物品不再公开，发布人奖励随之回滚
            rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);'''
new2 = '''            // 其他纠纷：判作废，物品不再公开，发布人奖励随之回滚
            rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);
            rollbackForwardReward(item, apply, reason, operatorId);'''
print('other match:', s.count(old2))
if s.count(old2) == 1:
    s = s.replace(old2, new2, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('done')
