<template>
  <div class="notice-detail-container" v-loading="loading">
    <el-card v-if="notice">
      <template #header>
        <div class="detail-header">
          <el-button link @click="$router.push('/notices')">
            <el-icon><ArrowLeft /></el-icon> 返回列表
          </el-button>
          <el-tag :type="NOTICE_STATUS_MAP[notice.status]?.type || 'info'">
            {{ NOTICE_STATUS_MAP[notice.status]?.text || '未知' }}
          </el-tag>
        </div>
      </template>

      <h2 class="notice-title">{{ notice.title }}</h2>
      <el-row :gutter="24">
        <el-col :span="10">
          <template v-if="notice.images && notice.images.length > 0">
            <el-image
              v-for="(img, idx) in notice.images"
              :key="idx"
              :src="img"
              fit="contain"
              style="width: 100%; max-height: 250px; margin-bottom: 8px"
              :preview-src-list="notice.images"
            />
          </template>
          <el-empty v-else description="无图片" :image-size="80" />
        </el-col>
        <el-col :span="14">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="分类">{{ notice.category }}</el-descriptions-item>
            <el-descriptions-item label="丢失地点">{{ notice.lostLocation || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="丢失时间">{{ formatTime(notice.lostTime) }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ notice.contactInfo }}</el-descriptions-item>
            <el-descriptions-item label="发布时间" :span="2">{{ formatTime(notice.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="物品描述" :span="2">
              {{ notice.description || '无描述' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            title="如果你在失物招领列表中看到相似物品，可以直接申请认领"
            type="info"
            :closable="false"
            style="margin-top: 16px"
          />
          <div class="action-bar">
            <el-button type="primary" plain @click="$router.push('/items')">去失物招领看看</el-button>
            <el-button
              v-if="isMyNotice && notice.status === 1"
              type="warning"
              :loading="acting"
              @click="handleClose"
            >
              关闭启事
            </el-button>
          </div>
          <section v-if="isMyNotice && notice.status === 0" class="matches-section">
            <div class="matches-heading">
              <div><h3>可能找到的物品</h3><p>匹配结果仅供参考，提交后仍由招领发布人核验</p></div>
              <el-button size="small" :loading="matchesLoading" @click="loadMatches">刷新匹配</el-button>
            </div>
            <el-empty v-if="!matchesLoading && !matches.length" description="暂无匹配物品" :image-size="56" />
            <div v-for="item in matches" :key="item.itemId" class="match-row">
              <div class="match-copy"><strong>{{ item.title }}</strong><span>{{ item.foundLocation || '地点未填写' }} · {{ item.category }}</span></div>
              <el-button size="small" type="primary" plain @click="openForward(item.itemId, item.claimQuestion)">这是我的</el-button>
            </div>
          </section>
        </el-col>
      </el-row>
    </el-card>
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

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-title {
  margin: 0 0 16px 0;
  color: #303133;
}

.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.matches-section { margin-top: 24px; padding-top: 18px; border-top: 1px solid #e5ebe7; }
.matches-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.matches-heading h3 { color: #34423b; font-size: 15px; }
.matches-heading p { margin-top: 4px; color: #89958f; font-size: 11px; }
.match-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 11px 0; border-bottom: 1px solid #edf0ee; }
.match-copy { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.match-copy strong { overflow: hidden; color: #34423b; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.match-copy span { color: #89958f; font-size: 11px; }
</style>
