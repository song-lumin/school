# -*- coding: utf-8 -*-
import io

# 1) 站点确认归还入库：原来承诺「重新发放拾获奖励」，但发布人 +3 是按物品幂等的，实际不会重发
p1 = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DropPointServiceImpl.java'
s = io.open(p1, encoding='utf-8').read()
old1 = ' + "」并由站点确认入库，现重新公开待认领，再次被正确认领时会重新发放拾获奖励。物品归属仍为你。",'
new1 = ' + "」并由站点确认入库，现重新公开待认领。拾获奖励按物品只结算一次，不会重复发放；下一位认领人仍会正常获得认领奖励。物品归属仍为你。",'
print('drop 1 match:', s.count(old1))
if s.count(old1) == 1:
    s = s.replace(old1, new1, 1)
io.open(p1, 'w', encoding='utf-8', newline='').write(s)

# 2) 纠纷通知里的死分支文案，同步改成不重发的口径，避免以后被复用造成误导
p2 = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
s2 = io.open(p2, encoding='utf-8').read()
old2 = '                        .append("再次被正确认领时会重新发放奖励。物品归属仍为你，不会转给误领人。");'
new2 = '                        .append("拾获奖励按物品只结算一次，不会重复发放。物品归属仍为你，不会转给误领人。");'
print('dispute 1 match:', s2.count(old2))
if s2.count(old2) == 1:
    s2 = s2.replace(old2, new2, 1)
io.open(p2, 'w', encoding='utf-8', newline='').write(s2)
print('done')
