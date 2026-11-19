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

rep('''        // 这次成交被判定无效，转发带来的 +1 一律撤回
        rollbackForwardReward(item, apply, reason, operatorId);''',
'''        // 这次成交被判定无效，转发带来的 +1 撤回；但损坏三行都写「转发不动」，不追溯损坏件的转发奖励
        if (!typeDamaged) {
            rollbackForwardReward(item, apply, reason, operatorId);
        }''', 'forward-guard')

rep('''            // 这次成交被判定无效，转发带来的 +1 一律撤回（与不符/冒领口径一致）
            rollbackForwardReward(item, apply, reason, operatorId);
            item.setItemStatus(ItemStatus.RETURNING.getCode());''',
'''            // 损坏件按规则表「转发不动」，这里不撤回转发奖励
            item.setItemStatus(ItemStatus.RETURNING.getCode());''', 'forward-damaged')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
