# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\ClaimServiceImpl.java'
s = io.open(p, encoding='utf-8').read()
old = '''            // 被暂停的申请人不告知就会一直干等；对方取消认领时会再推一条「已恢复待审核」
            for (ClaimApply other : othersLocked) {
                messageService.push(other.getClaimerId(), "你的认领申请已暂停",
                        "物品「" + item.getTitle() + "」已由他人认领审核通过，你的申请暂时无法继续。"
                                + "若对方到场后取消认领，你的申请会自动恢复为「待审核」，届时会再通知你。",
                        "CLAIM", item.getId(), other.getId());
            }'''
new = '''            // 被暂停的申请人不告知就会一直干等；对方现场退回时这条申请会一并作废，需要重新提交
            for (ClaimApply other : othersLocked) {
                messageService.push(other.getClaimerId(), "你的认领申请已暂停",
                        "物品「" + item.getTitle() + "」已由他人认领审核通过，你的申请暂时无法继续。"
                                + "若对方到场后取消认领，物品会重新回到「公开待认领」，但本次排队申请将一并作废，"
                                + "届时需要你重新提交认领。",
                        "CLAIM", item.getId(), other.getId());
            }'''
print('match=', s.count(old))
if s.count(old) == 1:
    s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
