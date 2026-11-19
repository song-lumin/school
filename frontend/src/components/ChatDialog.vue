<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="620px"
    append-to-body
    @update:model-value="onVisibleChange"
  >
    <el-alert
      type="info"
      :closable="false"
      style="margin-bottom: 10px"
      title="这条对话对「申诉人 · 该物品点位负责人 · 系统管理员」三方可见：问进度、报情况、催核验都走这里，比电话更容易留痕。"
    />
    <div ref="boxRef" class="chat-box">
      <div v-if="loading" class="chat-empty">加载中…</div>
      <template v-else-if="!messages.length">
        <div class="chat-empty">还没有沟通记录，先发一句说明情况。</div>
      </template>
      <div v-for="m in messages" :key="m.id" :class="['chat-row', m.mine ? 'right' : 'left']">
        <div class="chat-meta">
          {{ m.senderName || '用户' }} · {{ roleText(m.senderRole) }} · {{ fmtTime(m.createdAt) }}
        </div>
        <div :class="['bubble', m.mine ? 'bubble-mine' : 'bubble-other']">{{ m.content }}</div>
      </div>
    </div>
    <div class="chat-input">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        placeholder="想跟站点或系统管理员说什么？例如：监控回放看完了吗 / 我已把实物送回服务台。"
      />
      <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
    </div>
    <div class="chat-tip">对方收到站内信提醒；本工单处理完成后对话仍保留，可随时回来查看。</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api'

const props = withDefaults(defineProps<{
  visible: boolean
  disputeId?: number | null
  title?: string
}>(), {
  disputeId: null,
  title: '沟通'
})

const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const messages = ref<any[]>([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const boxRef = ref<HTMLElement>()
let timer: any = null

const roleText = (role?: string) => {
  if (role === 'SYS_ADMIN') return '系统管理员'
  if (role === 'POINT_ADMIN') return '站点负责人'
  return '用户'
}

const fmtTime = (t?: string) => {
  if (!t) return ''
  return t.replace('T', ' ').slice(5, 16)
}

const scrollBottom = async () => {
  await nextTick()
  if (boxRef.value) boxRef.value.scrollTop = boxRef.value.scrollHeight
}

const load = async () => {
  if (!props.disputeId) return
  loading.value = true
  try {
    const res = await chatApi.list({ disputeId: props.disputeId })
    messages.value = res.data || []
    await scrollBottom()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '读取沟通记录失败')
  } finally {
    loading.value = false
  }
}

const handleSend = async () => {
  if (!props.disputeId) return
  if (!draft.value.trim()) { ElMessage.warning('先写一句再发'); return }
  sending.value = true
  try {
    await chatApi.send({ disputeId: props.disputeId, content: draft.value.trim() })
    draft.value = ''
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const onVisibleChange = (v: boolean) => {
  emit('update:visible', v)
}

watch(() => props.visible, (v) => {
  if (v) {
    load()
    if (timer) clearInterval(timer)
    timer = setInterval(load, 5000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.chat-box {
  height: 320px;
  overflow-y: auto;
  padding: 8px 4px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
.chat-empty {
  color: #909399;
  font-size: 13px;
  padding: 24px 0;
  text-align: center;
}
.chat-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}
.chat-row.right { align-items: flex-end; }
.chat-row.left { align-items: flex-start; }
.chat-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.bubble {
  max-width: 78%;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble-mine { background: #fdeceb; color: #303133; }
.bubble-other { background: #ffffff; color: #303133; border: 1px solid #ebeef5; }
.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 10px;
}
.chat-input .el-textarea { flex: 1; }
.chat-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
