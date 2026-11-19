import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s = io.open(p, encoding='utf-8').read()

# 1) OTHER 分支（12 空格缩进）
old1 = '''            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
            // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）'''
new1 = '''            // 发布人奖励发放时 relatedApplyId 为 null（按物品一次性结算），
            // 回滚若带上 applyId 会查不到发放流水，等于永远不回滚；这里必须按物品维度定位
            rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), null, reason, operatorId);
            // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）'''

# 2) NOT_DELIVERED 分支（8 空格缩进）
old2 = '''        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), apply.getId(), reason, operatorId);
        // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）'''
new2 = '''        // 同上：发布人 +3 是按物品发的，回滚不带 applyId，否则永远匹配不到发放记录
        rollbackSingle(publisherId, "PICKUP_ISSUE", 3, item.getId(), null, reason, operatorId);
        // 巡检加分是在交物阶段发的、与认领申请无关，按物品维度定位（同一物品只巡检发过一次）'''

for old, new, tag in ((old1, new1, 'OTHER'), (old2, new2, 'NOT_DELIVERED')):
    cnt = s.count(old)
    print(tag, 'matches=', cnt)
    if cnt == 1:
        s = s.replace(old, new, 1)
    else:
        print('SKIP', tag)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('publisher rollback with null applyId occurrences:', s.count('"PICKUP_ISSUE", 3, item.getId(), null'))
