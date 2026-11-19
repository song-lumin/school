# -*- coding: utf-8 -*-
import io
base = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl'
p = base + r'\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(f'{tag}: match={c}')
    if c == 1:
        s = s.replace(old, new, 1)

# a) 不符 = 领错，成交无效 → 发布人奖励回滚；损坏仍保留（物品作废，不再产生新成交）
rep('''        // 不符/损坏的实物都要收回站点，属于「失物在站」，发布人奖励一律保留
        boolean publisherCreditsStay = typeMismatch || typeDamaged;''',
'''        // 损坏件实物收回后按定责作废，拾获奖励保留（物品已毁，不会再产生新的有效成交）；
        // 物品不符属于「领错」：这次成交无效，拾获奖励随认领作废回滚，实物重新公开由真失主认领时重新结算
        boolean publisherCreditsStay = typeDamaged;''', 'a-stay')

# b) 发布人回滚改为按申请粒度（发放已带 applyId）
rep('''            // 发布人奖励发放时 relatedApplyId 为 null（按物品一次性结算），
            // 回滚若带上 applyId 会查不到发放流水，等于永远不回滚；这里必须按物品维度定位
            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), null, reason, operatorId);''',
'''            // 发放已按「成交」绑定 applyId，这里按同一笔申请回滚，保证「领错→退回→真失主再取」可以再结算
            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);''', 'b-publisher')

# c) 冒领：发布人的拾获 +3 / 交物 +1 同属这次无效成交，一并回滚
rep('''        // 发布人无过错：他正常投放、正常交物，物品是被别人冒领走的，且实物还在站点会被收回，
        // 因此拾获 +3 与交物 +1 都保留（失物在站不回滚）。
        // 冒领人这次成交无效：他的 +1 回滚，转发带来的 +1 也一并撤回。
        rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);''',
'''        // 冒领人这次成交无效：他的 +1 回滚，转发带来的 +1 也一并撤回；
        // 发布人的拾获 +3 / 交物 +1 同属这次无效成交，一并回滚，实物收回重新公开后由真失主重新结算。
        Long publisherId = item.getActualFounderId() != null ? item.getActualFounderId() : item.getFounderId();
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        rollbackSingle(publisherId, "CHECK_ISSUE", 1, item.getId(), null, reason, operatorId);
        rollbackSingle(apply.getClaimerId(), "PICKUP_ISSUE", 1, item.getId(), apply.getId(), reason, operatorId);''', 'c-false-claim')

# c2) 冒领通知发布人：奖励已回滚
rep('''                "冒领人已被扣 5 分并计一次误领。", true, false);''',
'''                "冒领人已被扣 5 分并计一次误领；该次拾获奖励已回滚，实物收回重新公开后由真失主重新结算。", true, true);''', 'c2-notify')

# d) 从未投放：按申请回滚
rep('''        // 同上：发布人 +3 是按物品发的，回滚不带 applyId，否则永远匹配不到发放记录
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), null, reason, operatorId);''',
'''        // 发放按成交绑定了 applyId，这里按同一笔申请回滚
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);''', 'd-not-delivered')

# e) 不符通知发布人：这次奖励确实回滚了
rep('''                    "误领人不扣分，仅计一次误领。", true, false);''',
'''                    "误领人不扣分，仅计一次误领。", true, true);''', 'e-mismatch-notify')

# f) 文案：回滚分支要说明会重新结算
rep('''                        .append("拾获奖励按物品只结算一次，不会重复发放。物品归属仍为你，不会转给误领人。");''',
'''                        .append("真失主再次认领并取件时会重新结算拾获奖励。物品归属仍为你，不会转给误领人。");''', 'f-wording')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('DisputeServiceImpl saved')

# g) 站点归还入库文案
p2 = base + r'\DropPointServiceImpl.java'
s2 = io.open(p2, encoding='utf-8').read()
gd = '''                        + "」并由站点确认入库，现重新公开待认领。拾获奖励按物品只结算一次，不会重复发放；下一位认领人仍会正常获得认领奖励。物品归属仍为你。",'''
gnew = '''                        + "」并由站点确认入库，现重新公开待认领。此前被判无效的成交若已回滚拾获奖励，真失主再次认领取件时会重新结算；下一位认领人仍会正常获得认领奖励。物品归属仍为你。",'''
print('g-drop: match=', s2.count(gd))
if s2.count(gd) == 1:
    s2 = s2.replace(gd, gnew, 1)
    io.open(p2, 'w', encoding='utf-8', newline='').write(s2)
print('done step2')
