# -*- coding: utf-8 -*-
import io

p = r'D:\codex\codex-data\schoollose\frontend\src\views\FeedbackView.vue'
s = io.open(p, encoding='utf-8').read()

# 1) 在「我的纠纷申诉」表格末尾插入操作列（用 handlerNote 锚点定位，避免命中第二个表格）
anchor = '<el-table-column prop="handlerNote" label="处理意见" min-width="160" show-overflow-tooltip>'
i = s.find(anchor)
if i == -1:
    raise SystemExit('ABORT anchor1')
end = s.find('        </el-table>', i)
if end == -1:
    raise SystemExit('ABORT anchor2')
insert = """          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="warning" @click="openChat(row)">沟通</el-button>
            </template>
          </el-table-column>
"""
s = s[:end] + insert + s[end:]

# 表格下补一行说明
end2 = s.find('        </el-table>', end + len(insert)) + len('        </el-table>')
tip = """
        <div class="chat-tip-line">
          对工单处理有疑问（找不到物品、要调监控、催进度）可点「沟通」，直接跟该点位负责人和系统管理员留言。
        </div>"""
s = s[:end2] + tip + s[end2:]

# 2) 引入对话框
old2 = """    <!-- 纠纷申诉对话框 -->"""
if s.count(old2) != 1:
    raise SystemExit('ABORT dialog')
s = s.replace(old2, """    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 站点负责人与系统管理员" />

    <!-- 纠纷申诉对话框 -->""", 1)

# 3) 脚本变量
old3 = """const myDisputes = ref<DisputeVO[]>([])"""
if s.count(old3) != 1:
    raise SystemExit('ABORT script')
s = s.replace(old3, """const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const myDisputes = ref<DisputeVO[]>([])""", 1)

# 4) 引入组件
old4 = """import { disputeApi, claimApi, foundItemApi, reportApi, moderationApi } from '@/api'"""
if s.count(old4) != 1:
    raise SystemExit('ABORT import')
s = s.replace(old4, old4 + "\nimport ChatDialog from '@/components/ChatDialog.vue'", 1)

# 5) 样式
old5 = """<style scoped>"""
if s.count(old5) != 1:
    raise SystemExit('ABORT style')
s = s.replace(old5, """<style scoped>
.chat-tip-line {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}""", 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('FeedbackView patched ok')
