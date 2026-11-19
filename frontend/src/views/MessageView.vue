<template>
  <div class="message-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-line">
            <span class="title">站内消息</span>
            <el-tag v-if="unreadCount > 0" type="danger" size="small">{{ unreadCount }} 条未读</el-tag>
          </div>
          <el-button size="small" :disabled="unreadCount === 0" @click="handleReadAll">全部标为已读</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />

      <div v-else v-loading="loading" class="msg-list">
        <div
          v-for="m in messages"
          :key="m.id"
          class="msg-item"
          :class="{ unread: m.isRead === 0 }"
          @click="handleRead(m)"
        >
          <div class="msg-head">
            <el-tag size="small" :type="bizOf(m.bizType).type">{{ bizOf(m.bizType).text }}</el-tag>
            <span class="msg-title">{{ m.title }}</span>
            <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
          </div>
          <div class="msg-content">{{ m.content }}</div>
          <div class="msg-foot">
            <el-link
              v-if="m.relatedItemId"
              type="primary"
              :underline="false"
              @click.stop="$router.push(`/items/${m.relatedItemId}`)"
            >
              查看物品 #{{ m.relatedItemId }}
            </el-link>
            <el-link
              v-if="m.relatedItemId"
              type="info"
              :underline="false"
              style="margin-left: 14px"
              @click.stop="$router.push('/claims')"
            >
              我的认领
            </el-link>
            <span v-if="m.isRead === 0" class="unread-dot">未读</span>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

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
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi, messageApi } from '@/api'
import { MESSAGE_BIZ_MAP } from '@/types'
import type { SysMessage } from '@/types'
import ChatDialog from '@/components/ChatDialog.vue'

const chatVisible = ref(false)
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

const loading = ref(false)
const messages = ref<SysMessage[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const unreadCount = ref(0)

const bizOf = (bizType: string | null) =>
  (bizType && MESSAGE_BIZ_MAP[bizType]) || { text: bizType || '通知', type: 'info' as const }

const formatTime = (time?: string | null) => (time ? time.replace('T', ' ').slice(0, 16) : '-')

const fetchList = async () => {
  loading.value = true
  try {
    const res = await messageApi.list({ page: page.value, size: size.value })
    messages.value = res.data.records
    total.value = Number(res.data.total)
    await refreshUnread()
  } catch (e) {
    console.error('加载消息失败:', e)
  } finally {
    loading.value = false
  }
}

const refreshUnread = async () => {
  try {
    const res = await messageApi.unreadCount()
    unreadCount.value = Number(res.data.count || 0)
  } catch (e) {
    console.error(e)
  }
}

const handleRead = async (m: SysMessage) => {
  if (m.isRead === 1) return
  try {
    await messageApi.markRead(m.id)
    m.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) {
    console.error(e)
  }
}

const handleReadAll = async () => {
  try {
    await messageApi.markAllRead()
    messages.value.forEach((m) => (m.isRead = 1))
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchList()
  fetchThreads()
})
</script>

<style scoped>
.message-container {
  max-width: 1100px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-item {
  padding: 14px 16px;
  border: 1px solid #e4ebe6;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.msg-item:hover {
  border-color: #b9d6c7;
}

.msg-item.unread {
  background: #f7fbf8;
  border-left: 3px solid #26745c;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d27;
}

.msg-time {
  margin-left: auto;
  font-size: 12px;
  color: #8b9a92;
}

.msg-content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.75;
  color: #4a5650;
  white-space: pre-wrap;
}

.msg-foot {
  display: flex;
  align-items: center;
  margin-top: 10px;
  font-size: 12px;
}

.unread-dot {
  margin-left: auto;
  color: #c0392b;
  font-size: 12px;
}

.pagination {
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
}

@media (max-width: 640px) {
  .msg-time {
    width: 100%;
    margin-left: 0;
  }
}
</style>
