# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\frontend\src\types\index.ts'
s = io.open(p, encoding='utf-8').read()
old = """export interface DisputeHandleRequest {
  approved: boolean
  /** 找不到物品工单的核查结论：FOUND / NOT_DELIVERED / STOLEN */
  outcome?: string
  handlerNote: string
}"""
new = """export interface DisputeHandleRequest {
  approved: boolean
  /** 旧版按类型预设结论，前端已不再传 */
  outcome?: string
  /** 手动回滚项：PUBLISH_CHECK / PUBLISH_PICKUP / FORWARD / CLAIMER；传数组即走手动定责 */
  rollbackItems?: string[]
  /** 物品处置：NONE / VOIDED / LOST / RETURNING / PUBLIC */
  itemDispose?: string
  handlerNote: string
}"""
print('match=', s.count(old))
if s.count(old) != 1:
    raise SystemExit('ABORT')
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('types saved')
