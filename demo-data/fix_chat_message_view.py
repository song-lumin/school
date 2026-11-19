# -*- coding: utf-8 -*-
import io

# ---- 1) types：MESSAGE_BIZ_MAP 加 CHAT ----
p = r'D:\codex\codex-data\schoollose\frontend\src\types\index.ts'
s = io.open(p, encoding='utf-8').read()
old = """  ITEM_NOT_FOUND: { text: '找不到物品', type: 'info' }
}"""
new = """  ITEM_NOT_FOUND: { text: '找不到物品', type: 'info' },
  CHAT: { text: '沟通留言', type: 'warning' }
}"""
print('biz map', s.count(old))
if s.count(old) != 1:
    raise SystemExit('ABORT biz')
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('types saved')

# ---- 2) MessageView：加「我的沟通」卡片 ----
p2 = r'D:\codex\codex-data\schoollose\frontend\src\views\MessageView.vue'
s2 = io.open(p2, encoding='utf-8').read()

old2 = """    </el-card>
  </div>
</template>"""
new2 = """    </el-card>

    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="title-line">
            <span class="title">我的沟通</span>
            <el-tag size="small" type="warning">申诉工单对话</el-tag>
          </div>
          <el-button size="small" @click="fetchThreads">刷新</el-button>
        </div>
      </template>

      <el-empty
        v-if="!threadLoading && threads.length === 0"
        description="暂无沟通记录。在「我的申诉」的工单里点「沟通」即可发起"
      />
      <div v-else v-loading="threadLoading" class="thread-list">
        <div v-for="t in threads" :key="t.disputeId" class="thread-item" @click="openThread(t)">
          <div class="thread-head">
            <el-tag size="small" type="info">工单 #{{ t.disputeId }}</el-tag>
            <span class="thread-title">{{ t.itemTitle || ('物品 #' + t.itemId) }}</span>
            <el-tag size="small" :type="disputeTagType(t.status)">{{ disputeTagText(t.status) }}</el-tag>
          </div>
          <div class="thread-last">
            <template v-if="t.hasMessage">
              <span class="thread-sender">{{ t.lastSender }}：</span>{{ t.lastContent }}
            </template>
            <span v-else class="muted">还没有留言，点开可发起沟通</span>
          </div>
          <div class="thread-time">{{ formatTime(t.lastAt) }}</div>
        </div>
      </div>
    </el-card>

    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 工单对话" />
  </div>
</template>"""
print('msg card', s2.count(old2))
if s2.count(old2) != 1:
    raise SystemExit('ABORT msg card')
s2 = s2.replace(old2, new2, 1)

old3 = """const loading = ref(false)"""
new3 = """const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const threads = ref<any[]>([])
const threadLoading = ref(false)

const disputeTagText = (status?: number) =>
  status === 1 ? '已处理 · 通过' : status === 2 ? '已处理 · 驳回' : status === 3 ? '处理中' : '未处理'
const disputeTagType = (status?: number) =>
  (status === 1 ? 'success' : status === 2 ? 'danger' : status === 3 ? 'warning' : 'info') as any

const fetchThreads = async () => {
  threadLoading.value = true
  try {
    const res = await chatApi.threads()
    threads.value = res.data || []
  } catch (e) {
    console.error('加载沟通列表失败:', e)
  } finally {
    threadLoading.value = false
  }
}

const openThread = (t: any) => {
  chatDisputeId.value = t.disputeId
  chatVisible.value = true
}

const loading = ref(false)"""
print('msg script', s2.count(old3))
if s2.count(old3) != 1:
    raise SystemExit('ABORT msg script')
s2 = s2.replace(old3, new3, 1)

s2 = s2.replace("""import { messageApi } from '@/api'""",
                """import { chatApi, messageApi } from '@/api'""", 1)
s2 = s2.replace("""import type { SysMessage } from '@/types'""",
                """import type { SysMessage } from '@/types'
import ChatDialog from '@/components/ChatDialog.vue'""", 1)
s2 = s2.replace("""onMounted(fetchList)""", """onMounted(() => {
  fetchList()
  fetchThreads()
})""", 1)

old4 = """.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}"""
new4 = """.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.chat-card {
  margin-top: 16px;
}

.thread-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.thread-item {
  padding: 12px 14px;
  border: 1px solid #e4ebe6;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s;
}

.thread-item:hover {
  border-color: #b9d6c7;
}

.thread-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.thread-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d27;
}

.thread-last {
  margin-top: 6px;
  font-size: 13px;
  color: #4a5650;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.thread-sender {
  color: #26745c;
}

.muted {
  color: #9aa7a0;
}

.thread-time {
  margin-top: 6px;
  font-size: 12px;
  color: #8b9a92;
}"""
print('msg style', s2.count(old4))
if s2.count(old4) != 1:
    raise SystemExit('ABORT msg style')
s2 = s2.replace(old4, new4, 1)
io.open(p2, 'w', encoding='utf-8', newline='').write(s2)
print('MessageView saved')

# ---- 3) api：加 threads ----
p3 = r'D:\codex\codex-data\schoollose\frontend\src\api\index.ts'
s3 = io.open(p3, encoding='utf-8').read()
old5 = """export const chatApi = {
  list(params: { disputeId?: number | null; itemId?: number | null }) {"""
new5 = """export const chatApi = {
  threads() {
    return request.get<any, ApiResponse<any[]>>('/chat/threads')
  },

  list(params: { disputeId?: number | null; itemId?: number | null }) {"""
print('api threads', s3.count(old5))
if s3.count(old5) != 1:
    raise SystemExit('ABORT api threads')
s3 = s3.replace(old5, new5, 1)
io.open(p3, 'w', encoding='utf-8', newline='').write(s3)
print('api saved')
