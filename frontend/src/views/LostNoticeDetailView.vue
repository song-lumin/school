<template>
  <div class="notice-detail-container" v-loading="loading">
    <template v-if="notice">
      <div class="detail-back">
        <el-button link @click="$router.push('/notices')">
          <el-icon><ArrowLeft /></el-icon> 返回启事列表
        </el-button>
      </div>

      <div class="detail-panel">
        <div class="detail-visual" :class="`visual-${categoryTone(notice.category)}`">
          <template v-if="realImages(notice).length > 0">
            <el-image
              v-for="(img, idx) in realImages(notice)"
              :key="idx"
              :src="img"
              fit="cover"
              class="visual-image"
              :preview-src-list="realImages(notice)"
            >
              <template #error><div class="visual-fallback"><el-icon><Search /></el-icon></div></template>
            </el-image>
          </template>
          <div v-else class="visual-fallback">
            <el-icon><Search /></el-icon>
            <span>暂无图片</span>
          </div>
          <span class="visual-category">{{ notice.category }}</span>
        </div>

        <div class="detail-info">
          <div class="detail-status-row">
            <span class="status-chip" :data-status="notice.status">{{ NOTICE_STATUS_MAP[notice.status]?.text || '未知' }}</span>
          </div>
          <h1 class="detail-title">{{ notice.title }}</h1>
          <p v-if="notice.description" class="detail-desc">{{ notice.description }}</p>

          <dl class="detail-facts">
            <div class="fact"><dt>丢失地点</dt><dd><el-icon><Location /></el-icon>{{ notice.lostLocation || '未填写' }}</dd></div>
            <div class="fact"><dt>丢失时间</dt><dd>{{ formatTime(notice.lostTime) }}</dd></div>
            <div class="fact"><dt>联系方式</dt><dd>{{ notice.contactInfo }}</dd></div>
            <div class="fact"><dt>发布时间</dt><dd>{{ formatTime(notice.createdAt) }}</dd></div>
          </dl>

          <el-alert
            title="如果你在失物招领列表中看到相似物品，可以直接申请认领"
            type="info"
            :closable="false"
            style="margin-top: 20px"
          />
          <div class="action-bar">
            <el-button type="primary" plain @click="$router.push('/items')">去失物招领看看</el-button>
            <el-button
              v-if="isMyNotice && notice.status === 0"
              type="warning"
              :loading="acting"
              @click="handleClose"
            >
              关闭启事
            </el-button>
          </div>
        </div>
      </div>

      <section v-if="isMyNotice && notice.status === 0" class="matches-panel">
        <div class="matches-heading">
          <div><h2>可能找到的物品</h2><p>匹配结果仅供参考，提交后仍由招领发布人核验</p></div>
          <el-button size="small" :loading="matchesLoading" @click="loadMatches">刷新匹配</el-button>
        </div>
        <el-empty v-if="!matchesLoading && !matches.length" description="暂无匹配物品" :image-size="56" />
        <div v-for="item in matches" :key="item.itemId" class="match-row">
          <div class="match-copy"><strong>{{ item.title }}</strong><span>{{ item.foundLocation || '地点未填写' }} · {{ item.category }}</span></div>
          <el-button size="small" type="primary" plain @click="openForward(item.itemId, item.claimQuestion)">这是我的</el-button>
        </div>
      </section>
    </template>
    <el-dialog v-model="forwardDialogVisible" title="发起认领邀请" width="500px">
      <el-alert v-if="forwardQuestion" :title="forwardQuestion" type="info" :closable="false" style="margin-bottom: 16px" />
      <el-input v-model="forwardAnswer" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="回答招领发布人的核验问题，帮助确认物品属于你" />
      <template #footer>
        <el-button @click="forwardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="forwarding" @click="submitForward">发送邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lostNoticeApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { NOTICE_STATUS_MAP } from '@/types'
import type { LostNotice } from '@/types'
import { categoryTone } from '@/utils/placeholder'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const acting = ref(false)
const notice = ref<LostNotice | null>(null)
const matches = ref<Array<{ itemId: number; title: string; category: string; foundLocation: string; claimQuestion: string }>>([])
const matchesLoading = ref(false)
const forwardDialogVisible = ref(false)
const forwarding = ref(false)
const forwardItemId = ref<number | null>(null)
const forwardQuestion = ref('')
const forwardAnswer = ref('')

const noticeId = computed(() => route.params.id as string)
const isMyNotice = computed(() => {
  return !!notice.value && userStore.userInfo?.id === notice.value.publisherId
})

const isPlaceholderImage = (src: string) => src.includes('example.com')
const realImages = (notice: LostNotice) => (notice.images || []).filter(src => !isPlaceholderImage(src))

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await lostNoticeApi.getById(noticeId.value)
    notice.value = res.data
    if (isMyNotice.value && notice.value.status === 0) loadMatches()
  } catch (error) {
    console.error('加载启事详情失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMatches = async () => {
  matchesLoading.value = true
  try {
    matches.value = await lostNoticeApi.matches(noticeId.value).then((res) => res.data)
  } catch (error) {
    console.error('加载匹配物品失败:', error)
  } finally {
    matchesLoading.value = false
  }
}

const openForward = (itemId: number, question: string) => {
  forwardItemId.value = itemId
  forwardQuestion.value = question || '请描述物品特征以证明是你的'
  forwardAnswer.value = ''
  forwardDialogVisible.value = true
}

const submitForward = async () => {
  if (!forwardItemId.value || !forwardAnswer.value.trim()) {
    ElMessage.warning('请先填写核验答案')
    return
  }
  forwarding.value = true
  try {
    await lostNoticeApi.forward(noticeId.value, { itemId: forwardItemId.value, answer: forwardAnswer.value.trim() })
    ElMessage.success('认领邀请已发送，等待招领发布人核验')
    forwardDialogVisible.value = false
  } catch (error) {
    console.error('发送认领邀请失败:', error)
  } finally {
    forwarding.value = false
  }
}

const handleClose = async () => {
  try {
    await ElMessageBox.confirm('确定关闭这条寻物启事吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await lostNoticeApi.close(noticeId.value)
    ElMessage.success('已关闭')
    await fetchDetail()
  } catch (error) {
    console.error('关闭失败:', error)
  } finally {
    acting.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.notice-detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-back { margin: 2px 0 12px; }

.detail-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
  overflow: hidden;
  background: #fff;
  border: 1px solid #e2e9e4;
}

.detail-visual { position: relative; display: flex; flex-direction: column; gap: 10px; min-height: 420px; padding: 14px; overflow: hidden; background: #81a78e; }
.visual-image { flex: 1; min-height: 0; }
.visual-image :deep(.el-image__inner) { width: 100%; height: 100%; object-fit: cover; }
.visual-fallback { flex: 1; display: grid; place-content: center; justify-items: center; gap: 12px; width: 100%; color: rgb(255 255 255 / 85%); }
.visual-fallback .el-icon { font-size: 64px; }
.visual-fallback span { font-size: 13px; }
.visual-category { position: absolute; right: 14px; bottom: 14px; padding: 6px 11px; background: rgb(255 255 255 / 92%); color: #43564b; font-size: 12px; }
.visual-green { background: #81a78e; }
.visual-coral { background: #d88770; }
.visual-blue { background: #7899a5; }
.visual-yellow { background: #c3a668; }

.detail-info { display: flex; flex-direction: column; padding: 34px clamp(22px, 3.4vw, 44px) 32px; }
.detail-status-row { display: flex; flex-wrap: wrap; gap: 8px; }
.status-chip { padding: 5px 11px; font-size: 12px; font-weight: 600; color: #fff; }
.status-chip[data-status='0'] { background: #26745c; }
.status-chip[data-status='1'] { background: #5a738a; }
.status-chip[data-status='2'] { background: #78847c; }

.detail-title { margin: 16px 0 0; color: #26332f; font-size: clamp(22px, 2.2vw, 30px); font-weight: 650; line-height: 1.4; }
.detail-desc { margin: 14px 0 0; color: #5c6b63; font-size: 14px; line-height: 1.9; white-space: pre-wrap; }

.detail-facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 26px; margin: 24px 0 0; padding: 18px 0 0; border-top: 1px solid #e6ece8; }
.fact { display: flex; flex-direction: column; gap: 4px; padding: 9px 0; }
.fact dt { color: #8a968f; font-size: 12px; }
.fact dd { display: flex; align-items: center; gap: 5px; margin: 0; overflow: hidden; color: #2c3933; font-size: 14px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }
.fact dd .el-icon { flex: 0 0 auto; color: #6f8f7c; }

.action-bar { display: flex; flex-wrap: wrap; gap: 12px; margin-top: auto; padding-top: 26px; }

.matches-panel { margin-top: 22px; padding: 22px 24px; background: #fff; border: 1px solid #e2e9e4; }
.matches-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.matches-heading h2 { margin: 0; color: #26332f; font-size: 18px; font-weight: 650; }
.matches-heading p { margin-top: 4px; color: #89958f; font-size: 11px; }
.match-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 11px 0; border-bottom: 1px solid #edf0ee; }
.match-copy { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.match-copy strong { overflow: hidden; color: #34423b; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.match-copy span { color: #89958f; font-size: 11px; }

@media (max-width: 860px) {
  .detail-panel { grid-template-columns: 1fr; }
  .detail-visual { min-height: 280px; }
  .detail-info { padding: 24px 20px 26px; }
  .detail-facts { grid-template-columns: 1fr; }
}
</style>
