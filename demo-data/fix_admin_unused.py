# -*- coding: utf-8 -*-
import io, re
p = r'D:\codex\codex-data\schoollose\frontend\src\views\AdminView.vue'
s = io.open(p, encoding='utf-8').read()

old = """const approveEffectText = computed(() => {
  const type = handling.value?.disputeType
  if (type === 'FALSE_CLAIM') return '冒领人扣5分 + 计一次误领，物品转待归还，发布人奖励保留'
  if (type === 'ITEM_DAMAGED') return '物品转待归还（按定责方处理）'
  if (type === 'ITEM_MISMATCH') return '物品转待归还 + 认领人计一次误领'
  return '物品作废 + 回滚发布人奖励'
})

const DAMAGED_FAULT_TEXT: Record<string, string> = {
  SITE_FAULT: '定责站点保管责任：双方分不动，记站点考核',
  CLAIMER_FAULT: '定责认领人责任：回滚其 +1、计一次误领',
  PUBLISHER_FAULT: '定责发布人责任：扣发布人 2 分，认领人奖励保留'
}

"""
print('match=', s.count(old))
if s.count(old) != 1:
    raise SystemExit('ABORT')
s = s.replace(old, '', 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('removed unused helpers')
