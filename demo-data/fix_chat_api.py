# -*- coding: utf-8 -*-
import io

# ---------- 1) api：新增 chatApi ----------
p = r'D:\codex\codex-data\schoollose\frontend\src\api\index.ts'
s = io.open(p, encoding='utf-8').read()
old = """export const messageApi = {"""
new = """export const chatApi = {
  list(params: { disputeId?: number | null; itemId?: number | null }) {
    return request.get<any, ApiResponse<any[]>>('/chat/messages', { params })
  },

  send(data: { disputeId?: number | null; itemId?: number | null; content: string }) {
    return request.post<any, ApiResponse<any>>('/chat/messages', data)
  }
}

export const messageApi = {"""
print('api match=', s.count(old))
if s.count(old) != 1:
    raise SystemExit('ABORT api')
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('api saved')

# ---------- 2) 用户端申诉页：加沟通按钮 ----------
p2 = r'D:\codex\codex-data\schoollose\frontend\src\views\DisputeView.vue'
s2 = io.open(p2, encoding='utf-8').read()
print('DisputeView size', len(s2))
print('has import block:', 'from \'@/api\'' in s2 or 'from "@/api"' in s2)
print('el-table-column count', s2.count('<el-table-column'))
