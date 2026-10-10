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
            <span class="post-code-chip">启事编号 #{{ notice.id }}</span>
          </div>
          <h1 class="detail-title">{{ notice.title }}</h1>
          <p v-if="notice.description" class="detail-desc">{{ notice.description }}</p>

          <dl class="detail-facts">
            <div class="fact"><dt>丢失地点</dt><dd><el-icon><Location /></el-icon>{{ notice.lostLocation || '未填写' }}</dd></div>
            <div class="fact"><dt>丢失时间</dt><dd>{{ formatTime(notice.lostTime) }}</dd></div>
            <div class="fact"><dt>联系方式</dt><dd>{{ notice.contactInfo }}</dd></div>
            <div class="fact"><dt>发布者</dt><dd>{{ notice.publisherName || ('用户#' + notice.publisherId) }}</dd></div>
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
            <el-button v-if="isMyNotice && notice.status === 0" type="warning" :loading="acting" @click="handleClose">关闭启事</el-button>
            <el-button v-if="!isMyNotice && notice.status === 0" size="small" plain @click="reportDialogVisible = true">举报此帖</el-button>
            <el-button v-if="isMyNotice && notice.takedownReason && notice.appealStatus !== 1 && notice.appealStatus !== 2" size="small" type="warning" plain @click="appealDialogVisible = true">申诉恢复</el-button>
            <el-button v-if="userStore.isAdmin && notice.status === 0" size="small" type="danger" plain @click="adminTakedownDialogVisible = true">管理员下架</el-button>
          </div>
          <el-alert v-if="notice.takedownReason" :title="'该启事已被下架：' + notice.takedownReason" type="error" :closable="false" style="margin-top: 12px" />
          <el-alert v-if="notice.appealStatus === 1" title="申诉审核中" type="warning" :closable="false" style="margin-top: 10px" />
        </div>
      </div>

      <section v-if="forwardedItems.length" class="forwarded-panel">
        <div class="forwarded-heading">
          <h2>有人把这些招领转发到这里</h2>
          <p>拾得者认为这些物品可能与本条启事相关，启事发布者可直接点击进入答题认领；其他同学可在招领列表中搜索认领</p>
        </div>
        <div v-for="item in forwardedItems" :key="item.id" class="forwarded-row">
          <el-image
            v-if="item.images && item.images.length"
            :src="item.images[0]"
            fit="cover"
            style="width: 56px; height: 56px; border-radius: 6px"
          />
          <div v-else class="forwarded-noimg">无图</div>
          <div class="forwarded-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.foundLocation || '地点未填写' }} · {{ item.category }} · 发布于 {{ formatTime(item.publishedAt) }}</span>
          </div>
          <el-button v-if="isMyNotice" size="small" type="primary" @click="$router.push(`/items/${item.id}`)">去认领答题</el-button>
          <span v-else class="forwarded-only-hint">其他同学请到公共招领列表申请</span>
        </div>
      </section>


      <section v-if="userStore.isLoggedIn" class="forward-in-panel">
        <div class="forward-in-heading">
          <h2>按招领编号转发到本启事</h2>
          <p>如果你看到一条招领认为与本启事相关，输入招领编号即可把它转发到这里，启事发布者会看到并答题认领。</p>
        </div>
        <div class="forward-in-bar">
          <el-input v-model="forwardItemCode" placeholder="输入招领编号（如 13）" style="width:200px" clearable @keyup.enter="lookupForwardItem" />
          <el-button @click="lookupForwardItem">查找招领</el-button>
        </div>
        <el-card v-if="forwardItemPreview" shadow="never" class="forward-in-preview">
          <div><b>{{ forwardItemPreview.title }}</b> <el-tag size="small" style="margin-left:6px">招领编号 #{{ forwardItemPreview.id }}</el-tag></div>
          <div style="color:#666;font-size:13px;margin-top:6px">拾获地点：{{ forwardItemPreview.foundLocation }}　|　分类：{{ forwardItemPreview.category }}</div>
          <div style="margin-top:10px;text-align:right">
            <el-button size="small" @click="forwardItemPreview = null">取消</el-button>
            <el-button size="small" type="primary" :loading="acting" @click="confirmForwardItem">确认转发到本启事</el-button>
          </div>
        </el-card>
      </section>
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

    <el-dialog v-model="reportDialogVisible" title="举报此寻物启事" width="480px">
      <el-select v-model="reportType" placeholder="举报类型" style="width:100%;margin-bottom:12px">
        <el-option label="虚假信息" value="FAKE_PUBLISH" />
        <el-option label="描述不符" value="DESC_MISMATCH" />
        <el-option label="其他" value="OTHER" />
      </el-select>
      <el-input v-model="reportDesc" type="textarea" :rows="3" placeholder="请描述问题" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealDialogVisible" title="申诉恢复" width="480px">
      <el-input v-model="appealReasonText" type="textarea" :rows="3" placeholder="申诉理由" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminTakedownDialogVisible" title="管理员下架" width="480px">
      <el-input v-model="adminTakedownReason" type="textarea" :rows="3" placeholder="下架原因" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="adminTakedownDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="submitAdminTakedown">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lostNoticeApi, reportApi, moderationApi, foundItemApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { NOTICE_STATUS_MAP } from '@/types'
import type { LostNotice, FoundItem } from '@/types'
import { categoryTone } from '@/utils/placeholder'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const acting = ref(false)
const notice = ref<LostNotice | null>(null)
const forwardedItems = ref<FoundItem[]>([])
const forwardItemCode = ref('')
const forwardItemPreview = ref<any>(null)

const reportDialogVisible = ref(false)
const reportType = ref('OTHER')
const reportDesc = ref('')
const appealDialogVisible = ref(false)
const appealReasonText = ref('')
const adminTakedownDialogVisible = ref(false)
const adminTakedownReason = ref('')
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
    loadForwardedItems()
    if (isMyNotice.value && notice.value.status === 0) loadMatches()
  } catch (error) {
    console.error('加载启事详情失败:', error)
  } finally {
    loading.value = false
  }
}

const lookupForwardItem = async () => {
  const code = parseInt(forwardItemCode.value, 10)
  if (!code || isNaN(code)) {
    ElMessage.warning('请输入正确的招领编号')
    return
  }
  try {
    const res = await foundItemApi.getById(code)
    forwardItemPreview.value = res.data
  } catch {
    ElMessage.error('未找到招领编号 ' + code)
    forwardItemPreview.value = null
  }
}
const confirmForwardItem = async () => {
  if (!forwardItemPreview.value) return
  acting.value = true
  try {
    await foundItemApi.forwardToNotice(forwardItemPreview.value.id, Number(noticeId.value))
    ElMessage.success('已转发到本启事')
    forwardItemPreview.value = null
    forwardItemCode.value = ''
    loadForwardedItems()
  } catch (e) {
    console.error('转发失败', e)
  } finally {
    acting.value = false
  }
}
const loadForwardedItems = async () => {
  try {
    const res = await lostNoticeApi.forwardedItems(noticeId.value)
    forwardedItems.value = res.data
  } catch {
    forwardedItems.value = []
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

const submitReport = async () => {
  if (!reportDesc.value.trim()) { ElMessage.warning('请填写描述'); return }
  acting.value = true
  try {
    await reportApi.create({ reportType: reportType.value, targetType: 'NOTICE', noticeId: Number(noticeId.value), description: reportDesc.value.trim() })
    ElMessage.success('举报已提交')
    reportDialogVisible.value = false
  } catch (e) { console.error(e) } finally { acting.value = false }
}

const submitAppeal = async () => {
  if (!appealReasonText.value.trim()) { ElMessage.warning('请填写理由'); return }
  acting.value = true
  try {
    await moderationApi.appealNotice(noticeId.value, appealReasonText.value.trim())
    ElMessage.success('申诉已提交')
    appealDialogVisible.value = false
    await fetchDetail()
  } catch (e) { console.error(e) } finally { acting.value = false }
}

const submitAdminTakedown = async () => {
  if (!adminTakedownReason.value.trim()) { ElMessage.warning('请填写原因'); return }
  acting.value = true
  try {
    await moderationApi.takedownNotice(noticeId.value, adminTakedownReason.value.trim())
    ElMessage.success('已下架')
    adminTakedownDialogVisible.value = false
    await fetchDetail()
  } catch (e) { console.error(e) } finally { acting.value = false }
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
.post-code-chip { display:inline-block; padding:2px 8px; background:#eef4f0; color:#26745c; border:1px solid #cfe3d8; border-radius:4px; font-size:12px; font-weight:600; }
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

.forwarded-panel { margin-top: 22px; padding: 22px 24px; background: #fff; border: 1px solid #d4e8d9; border-left: 4px solid #67c23a; }
.forwarded-heading h2 { margin: 0; color: #26332f; font-size: 18px; font-weight: 650; }
.forwarded-heading p { margin-top: 4px; color: #89958f; font-size: 12px; }
.forwarded-row { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #edf0ee; }
.forwarded-noimg { width: 56px; height: 56px; border-radius: 6px; background: #f0f2f5; display: grid; place-content: center; color: #909399; font-size: 12px; flex-shrink: 0; }
.forwarded-copy { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 0; }
.forwarded-copy strong { color: #34423b; font-size: 14px; }
.forwarded-copy span { color: #89958f; font-size: 12px; }
.forwarded-only-hint { color: #b0b8b3; font-size: 12px; }
.forward-in-panel { margin-top: 18px; padding: 18px 22px; background: #fff; border: 1px solid #e2e9e4; border-radius: 8px; }
.forward-in-heading h2 { margin: 0; font-size: 16px; color: #26332f; }
.forward-in-heading p { margin: 4px 0 12px; color: #89958f; font-size: 12px; }
.forward-in-bar { display: flex; gap: 10px; align-items: center; }
.forward-in-preview { margin-top: 12px; }
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
