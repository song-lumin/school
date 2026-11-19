# -*- coding: utf-8 -*-
import io

# ---- 1) 状态映射加「处理中」----
p1 = r'D:\codex\codex-data\schoollose\frontend\src\types\index.ts'
s1 = io.open(p1, encoding='utf-8').read()
def rep1(old, new, tag):
    global s1
    c = s1.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s1 = s1.replace(old, new, 1)

rep1('''  0: { text: '待处理', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }''',
'''  0: { text: '未处理', type: 'warning' },
  1: { text: '已处理 · 通过', type: 'success' },
  2: { text: '已处理 · 驳回', type: 'danger' },
  3: { text: '处理中', type: 'primary' }''', 'status-map')
io.open(p1, 'w', encoding='utf-8', newline='').write(s1)

# ---- 2) 管理员端：筛选加处理中；处理按钮对处理中也可用 ----
p2 = r'D:\codex\codex-data\schoollose\frontend\src\views\AdminView.vue'
s2 = io.open(p2, encoding='utf-8').read()
def rep2(old, new, tag):
    global s2
    c = s2.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s2 = s2.replace(old, new, 1)

rep2('''              <el-option label="待处理" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />''',
'''              <el-option label="未处理" :value="0" />
              <el-option label="处理中（站点已接手）" :value="3" />
              <el-option label="已处理 · 通过" :value="1" />
              <el-option label="已处理 · 驳回" :value="2" />''', 'admin-filter')

rep2('''                <el-button link type="primary" @click="openHandleDialog(row)">处理</el-button>''',
'''                <el-button link type="primary" @click="openHandleDialog(row)">处理</el-button>
                <el-tag v-if="row.status === 3" size="small" type="primary" style="margin-left:6px">待定责</el-tag>''', 'admin-action')

io.open(p2, 'w', encoding='utf-8', newline='').write(s2)
print('frontend saved')
