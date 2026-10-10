<template>
  <div class="detail-container" v-loading="loading">
    <template v-if="item">
      <div class="detail-back">
        <el-button link @click="$router.push('/items')">
          <el-icon><ArrowLeft /></el-icon> 返回招领列表
        </el-button>
      </div>

      <div class="detail-panel">
        <div class="detail-visual" :class="`visual-${categoryTone(item.category)}`">
          <template v-if="realImages(item).length > 0">
            <el-carousel
              v-if="realImages(item).length > 1"
              height="100%"
              indicator-position="outside"
              class="visual-carousel"
            >
              <el-carousel-item v-for="(img, idx) in realImages(item)" :key="idx">
                <el-image :src="img" fit="cover" style="width: 100%; height: 100%" :preview-src-list="realImages(item)">
                  <template #error><div class="visual-fallback"><el-icon><Box /></el-icon></div></template>
                </el-image>
              </el-carousel-item>
            </el-carousel>
            <el-image v-else :src="realImages(item)[0]" fit="cover" class="visual-image" :preview-src-list="realImages(item)">
              <template #error><div class="visual-fallback"><el-icon><Box /></el-icon></div></template>
            </el-image>
          </template>
          <div v-else class="visual-fallback">
            <el-icon><Box /></el-icon>
            <span>暂无图片</span>
          </div>
          <span class="visual-category">{{ item.category }}</span>
        </div>

        <div class="detail-info">
          <div class="detail-status-row">
            <span class="status-chip" :data-status="item.itemStatus">{{ ITEM_STATUS_MAP[item.itemStatus]?.text || '未知' }}</span>
            <span v-if="item.perishable === 1" class="perish-chip">易腐品</span>
            <span class="post-code-chip">招领编号 #{{ item.id }}</span>
            <span v-if="item.newUser" class="newbie-chip">新手发布</span>
          </div>
          <h1 class="detail-title">{{ item.title }}</h1>
          <p v-if="item.description" class="detail-desc">{{ item.description }}</p>

          <dl class="detail-facts">
            <div class="fact"><dt>拾取地点</dt><dd><el-icon><Location /></el-icon>{{ item.foundLocation || '未填写' }}</dd></div>
            <div class="fact"><dt>拾取时间</dt><dd>{{ formatTime(item.foundTime) }}</dd></div>
            <div class="fact"><dt>发布人</dt><dd>{{ item.founderName || '匿名' }}</dd></div>
            <div class="fact"><dt>发布时间</dt><dd>{{ formatTime(item.publishedAt) }}</dd></div>
            <div class="fact fact-wide"><dt>存放投放点</dt><dd>{{ item.dropPointName || '未指定' }}</dd></div>
            <div v-if="item.claimQuestion" class="fact fact-wide"><dt>防伪问题</dt><dd>{{ item.claimQuestion }}</dd></div>
          </dl>

          <div class="action-bar" v-if="userStore.isLoggedIn">
            <template v-if="isMyItem">
              <el-button
                v-if="item.itemStatus === 1"
                type="danger"
                plain
                :loading="acting"
                @click="handleInvalidate"
              >
                作废
              </el-button>
            </template>
            <template v-else-if="item.itemStatus === 1">
              <el-button type="primary" size="large" @click="claimDialogVisible = true">这是我的，申请认领</el-button>
            </template>
            <el-button
              v-if="item.itemStatus === 1"
              type="warning"
              plain
              :loading="acting"
              @click="openForwardDialog"
            >
              转发到寻物启事
            </el-button>
            <el-button
              v-if="(isMyItem && item.itemStatus === 3) || (userStore.isAdmin && item.itemStatus === 5)"
              type="info"
              plain
              :loading="acting"
              @click="handleArchive"
            >
              {{ item.itemStatus === 5 ? '处置归档' : '归档' }}
            </el-button>
          </div>

          <el-alert
            v-if="item.takedownReason"
            :title="'该帖已被下架：' + item.takedownReason"
            type="error"
            :closable="false"
            style="margin-top: 16px"
          />
          <el-alert
            v-if="item.appealStatus === 1"
            title="申诉审核中，请等待管理员处理"
            type="warning"
            :closable="false"
            style="margin-top: 12px"
          />

          <div class="action-bar" style="margin-top: 12px">
            <el-button v-if="!isMyItem && item.itemStatus === 1" size="small" plain @click="reportDialogVisible = true">举报此帖</el-button>
            <el-button v-if="isMyItem && item.takedownReason && item.appealStatus !== 1 && item.appealStatus !== 2" size="small" type="warning" plain @click="appealDialogVisible = true">申诉恢复</el-button>
            <el-button v-if="userStore.isAdmin && item.itemStatus === 1" size="small" type="danger" plain @click="adminTakedownDialogVisible = true">管理员下架</el-button>
          </div>

          <el-alert
            v-if="item.itemStatus === 1 && !userStore.isLoggedIn"
            title="登录后可申请认领"
            type="info"
            :closable="false"
            style="margin-top: 20px"
          />
        </div>
      </div>

      <div v-if="canSeeClaims" class="claims-panel">
        <div class="claims-heading">
          <h2>{{ isMyItem ? '收到的认领申请' : '该物品的认领申请（管理员视图）' }}</h2>
          <el-button link type="primary" @click="$router.push('/claims')">全部管理 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <el-empty description="暂无申请" v-if="claims.length === 0" :image-size="60" />
        <el-table v-else :data="claims" size="small">
          <el-table-column prop="claimerName" label="申请人" width="110" />
          <el-table-column label="答案与审核提示" min-width="220">
            <template #default="{ row }">
              <div class="answer-review"><span>{{ row.answer }}</span><el-tag v-if="row.lowConfidence === 1" size="small" type="warning">低置信度建议</el-tag><small v-if="row.lowConfidence === 1">{{ row.confidenceReason }}</small></div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="130">
            <template #default="{ row }">
              <el-link v-if="row.sourceNoticeId" type="primary" @click="$router.push(`/notices/${row.sourceNoticeId}`)">启事 #{{ row.sourceNoticeId }}</el-link>
              <span v-else>直接申请</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="applyStatus" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="CLAIM_STATUS_MAP[row.applyStatus]?.type || 'info'">
                {{ CLAIM_STATUS_MAP[row.applyStatus]?.text || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <template v-if="row.applyStatus === 0">
                <el-button link type="success" @click="handleApprove(row)">同意</el-button>
                <el-button link type="danger" @click="openRejectDialog(row)">驳回</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-dialog v-model="claimDialogVisible" title="申请认领" width="500px">
      <el-alert
        v-if="item?.claimQuestion"
        :title="`防伪问题：${item.claimQuestion}`"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-input
        v-model="claimAnswer"
        type="textarea"
        :rows="3"
        :placeholder="item?.claimQuestion ? '请输入问题答案' : '请描述物品特征以证明是你的'"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="claimDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="claiming" @click="handleClaim">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="驳回认领" width="450px">
      <el-input v-model="rejectReason" type="textarea" :rows="2" placeholder="请输入驳回原因" maxlength="200" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="forwardDialogVisible" title="转发到寻物启事" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px">
        输入寻物启事编号定位后核对，确认无误再转发。
      </el-alert>

      <div class="forward-code-bar">
        <el-input v-model="forwardCodeInput" placeholder="输入寻物启事编号（如 7）" style="width: 220px" clearable @keyup.enter="lookupForwardCode" />
        <el-button @click="lookupForwardCode">按编号查找</el-button>
      </div>

      <el-card v-if="forwardPreview" class="forward-preview" shadow="never">
        <template #header><span style="font-weight:600">核对这条启事：</span></template>
        <div><b>{{ forwardPreview.title }}</b> <el-tag size="small" style="margin-left:6px">编号 #{{ forwardPreview.id }}</el-tag></div>
        <div style="color:#666;font-size:13px;margin-top:6px">丢失地点：{{ forwardPreview.lostLocation }}　|　丢失时间：{{ forwardPreview.lostTime || '-' }}</div>
        <div style="margin-top:10px;text-align:right">
          <el-button size="small" @click="forwardPreview = null">取消</el-button>
          <el-button size="small" type="primary" :loading="acting" @click="doForward(forwardPreview.id)">确认转发到此启事</el-button>
        </div>
      </el-card>

    </el-dialog>

    <el-dialog v-model="reportDialogVisible" title="举报此招领" width="480px">
      <el-select v-model="reportType" placeholder="选择举报类型" style="width: 100%; margin-bottom: 12px">
        <el-option label="虚假投放" value="FAKE_PUBLISH" />
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
      <el-alert title="请说明为什么这篇招领应该被恢复" type="info" :closable="false" style="margin-bottom: 12px" />
      <el-input v-model="appealReasonText" type="textarea" :rows="3" placeholder="申诉理由" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminTakedownDialogVisible" title="管理员下架" width="480px">
      <el-alert title="下架必须填写原因，发布者可看到原因并申诉" type="warning" :closable="false" style="margin-bottom: 12px" />
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
import { foundItemApi, claimApi, lostNoticeApi, reportApi, moderationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ITEM_STATUS_MAP, CLAIM_STATUS_MAP } from '@/types'
import type { FoundItem, ClaimApply, LostNotice } from '@/types'
import { categoryTone } from '@/utils/placeholder'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const acting = ref(false)
const item = ref<FoundItem | null>(null)
const claims = ref<ClaimApply[]>([])
const claimsForbidden = ref(false)

const claimDialogVisible = ref(false)
const claimAnswer = ref('')
const claiming = ref(false)

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectingClaim = ref<ClaimApply | null>(null)

const forwardDialogVisible = ref(false)
const noticesLoading = ref(false)
const openNotices = ref<LostNotice[]>([])
const forwardCodeInput = ref('')
const forwardPreview = ref<any>(null)

const reportDialogVisible = ref(false)
const reportType = ref('OTHER')
const reportDesc = ref('')
const appealDialogVisible = ref(false)
const appealReasonText = ref('')
const adminTakedownDialogVisible = ref(false)
const adminTakedownReason = ref('')

const itemId = computed(() => route.params.id as string)
const isMyItem = computed(() => {
  return !!item.value && userStore.userInfo?.id === item.value.founderId
})
const isReviewer = computed(() => isMyItem.value || userStore.isAdmin || userStore.isPointAdmin)
const canSeeClaims = computed(() => !!item.value && isReviewer.value && item.value.itemStatus !== 0 && !claimsForbidden.value)

const isPlaceholderImage = (src: string) => src.includes('example.com')
const realImages = (item: FoundItem) => (item.images || []).filter(src => !isPlaceholderImage(src))

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await foundItemApi.getById(itemId.value)
    item.value = res.data
    if (canSeeClaims.value) {
      await fetchClaims()
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchClaims = async () => {
  if (!item.value) return
  try {
    const res = await claimApi.listByItem(itemId.value, { page: 1, size: 20 })
    claims.value = res.data.records
  } catch (error) {
    console.error('加载认领申请失败:', error)
  }
}

const handleInvalidate = async () => {
  try {
    await ElMessageBox.confirm('作废后该招领将不可恢复，确定作废吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await foundItemApi.invalidate(itemId.value)
    ElMessage.success('已作废')
    await fetchDetail()
  } catch (error) {
    console.error('作废失败:', error)
  } finally {
    acting.value = false
  }
}

const handleArchive = async () => {
  const tip = item.value?.itemStatus === 5
    ? '确认已对超期物品完成线下处置（捐赠、销毁等）？归档后进入历史记录。'
    : '归档后该招领进入历史记录，确定归档吗？'
  try {
    await ElMessageBox.confirm(tip, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  acting.value = true
  try {
    await foundItemApi.archive(itemId.value)
    ElMessage.success('已归档')
    await fetchDetail()
  } catch (error) {
    console.error('归档失败:', error)
  } finally {
    acting.value = false
  }
}

const handleClaim = async () => {
  if (!claimAnswer.value.trim()) {
    ElMessage.warning('请输入答案')
    return
  }
  claiming.value = true
  try {
    await claimApi.apply({ itemId: Number(itemId.value), answer: claimAnswer.value })
    ElMessage.success('申请已提交，等待发布人审核')
    claimDialogVisible.value = false
    claimAnswer.value = ''
  } catch (error) {
    console.error('认领申请失败:', error)
  } finally {
    claiming.value = false
  }
}

const handleApprove = async (row: ClaimApply) => {
  acting.value = true
  try {
    await claimApi.approve(row.id)
    ElMessage.success('已同意，请失主到投放点领取')
    await fetchClaims()
  } catch (error) {
    console.error('同意失败:', error)
  } finally {
    acting.value = false
  }
}

const openRejectDialog = (row: ClaimApply) => {
  rejectingClaim.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  if (!rejectingClaim.value) return
  acting.value = true
  try {
    await claimApi.reject(rejectingClaim.value.id, { rejectReason: rejectReason.value })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    await fetchClaims()
  } catch (error) {
    console.error('驳回失败:', error)
  } finally {
    acting.value = false
  }
}

const openForwardDialog = async () => {
  forwardDialogVisible.value = true
  forwardPreview.value = null
  forwardCodeInput.value = ''
  noticesLoading.value = true
  try {
    const res = await lostNoticeApi.list({ status: 0, page: 1, size: 50 })
    openNotices.value = res.data.records
  } catch {
    openNotices.value = []
  } finally {
    noticesLoading.value = false
  }
}

const lookupForwardCode = () => {
  const code = parseInt(forwardCodeInput.value, 10)
  if (!code || isNaN(code)) {
    ElMessage.warning('请输入正确的启事编号')
    return
  }
  const found = openNotices.value.find(n => n.id === code)
  if (found) {
    forwardPreview.value = found
  } else {
    ElMessage.error('未找到编号 ' + code + ' 的进行中寻物启事，请核对编号')
    forwardPreview.value = null
  }
}

const doForward = async (noticeId: number) => {
  acting.value = true
  try {
    await foundItemApi.forwardToNotice(itemId.value, noticeId)
    ElMessage.success('已转发到寻物启事，启事发布者将看到这条招领')
    forwardDialogVisible.value = false
  } catch (error) {
    console.error('转发失败:', error)
  } finally {
    acting.value = false
  }
}

const submitReport = async () => {
  if (!reportDesc.value.trim()) { ElMessage.warning('请填写举报描述'); return }
  acting.value = true
  try {
    await reportApi.create({ reportType: reportType.value, itemId: Number(itemId.value), description: reportDesc.value.trim() })
    ElMessage.success('举报已提交，等待管理员审核')
    reportDialogVisible.value = false
    reportDesc.value = ''
  } catch (e) { console.error(e) } finally { acting.value = false }
}

const submitAppeal = async () => {
  if (!appealReasonText.value.trim()) { ElMessage.warning('请填写申诉理由'); return }
  acting.value = true
  try {
    await moderationApi.appealItem(itemId.value, appealReasonText.value.trim())
    ElMessage.success('申诉已提交')
    appealDialogVisible.value = false
    await fetchDetail()
  } catch (e) { console.error(e) } finally { acting.value = false }
}

const submitAdminTakedown = async () => {
  if (!adminTakedownReason.value.trim()) { ElMessage.warning('请填写下架原因'); return }
  acting.value = true
  try {
    await moderationApi.takedownItem(itemId.value, adminTakedownReason.value.trim())
    ElMessage.success('已下架')
    adminTakedownDialogVisible.value = false
    await fetchDetail()
  } catch (e) { console.error(e) } finally { acting.value = false }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.detail-container {
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

.detail-visual { position: relative; min-height: 420px; overflow: hidden; background: #81a78e; }
.visual-carousel, .visual-carousel :deep(.el-carousel__container) { height: 100%; }
.visual-image { display: flex; width: 100%; height: 100%; }
.visual-image :deep(.el-image__inner) { width: 100%; height: 100%; }
.detail-visual :deep(.el-image) { width: 100%; height: 100%; }
.detail-visual :deep(.el-carousel__indicator) { --el-carousel-indicator-out-color: rgb(255 255 255 / 70%); }
.visual-fallback { display: grid; place-content: center; justify-items: center; gap: 12px; width: 100%; height: 100%; color: rgb(255 255 255 / 85%); }
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
.status-chip[data-status='1'] { background: #26745c; }
.status-chip[data-status='2'] { background: #b3661d; }
.status-chip[data-status='3'] { background: #5a738a; }
.status-chip[data-status='0'], .status-chip[data-status='4'], .status-chip[data-status='5'] { background: #78847c; }
.perish-chip { padding: 5px 11px; background: #f6e3d8; color: #a04b32; font-size: 12px; font-weight: 550; }
.post-code-chip { display:inline-block; padding:2px 8px; background:#eef4ff0; color:#26745c; border:1px solid #cfe3d8; border-radius:4px; font-size:12px; font-weight:600; }
.newbie-chip { padding: 5px 11px; background: #f3e9cf; color: #8d6d1f; font-size: 12px; font-weight: 550; }

.detail-title { margin: 16px 0 0; color: #26332f; font-size: clamp(22px, 2.2vw, 30px); font-weight: 650; line-height: 1.4; }
.detail-desc { margin: 14px 0 0; color: #5c6b63; font-size: 14px; line-height: 1.9; white-space: pre-wrap; }

.detail-facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 26px; margin: 24px 0 0; padding: 18px 0 0; border-top: 1px solid #e6ece8; }
.fact { display: flex; flex-direction: column; gap: 4px; padding: 9px 0; }
.fact-wide { grid-column: 1 / -1; }
.fact dt { color: #8a968f; font-size: 12px; }
.fact dd { display: flex; align-items: center; gap: 5px; margin: 0; color: #2c3933; font-size: 14px; font-weight: 550; }
.fact dd .el-icon { color: #6f8f7c; }

.action-bar { display: flex; flex-wrap: wrap; gap: 12px; margin-top: auto; padding-top: 26px; }

.claims-panel { margin-top: 22px; padding: 22px 24px; background: #fff; border: 1px solid #e2e9e4; }
.claims-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.claims-heading h2 { margin: 0; color: #26332f; font-size: 18px; font-weight: 650; }

.answer-review { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.answer-review small { color: #ad7a26; font-size: 11px; }

@media (max-width: 860px) {
  .detail-panel { grid-template-columns: 1fr; }
  .detail-visual { min-height: 280px; }
  .detail-info { padding: 24px 20px 26px; }
  .detail-facts { grid-template-columns: 1fr; }
}
</style>
