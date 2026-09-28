<template>
  <div class="detail-container" v-loading="loading">
    <el-card v-if="item">
      <template #header>
        <div class="detail-header">
          <div class="header-left">
            <el-button link @click="$router.push('/items')">
              <el-icon><ArrowLeft /></el-icon> 返回列表
            </el-button>
          </div>
          <el-tag :type="ITEM_STATUS_MAP[item.itemStatus]?.type || 'info'">
            {{ ITEM_STATUS_MAP[item.itemStatus]?.text || '未知' }}
          </el-tag>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :span="10">
          <template v-if="item.images && item.images.length > 0">
            <el-carousel
              v-if="item.images.length > 1"
              height="300px"
              indicator-position="outside"
            >
              <el-carousel-item v-for="(img, idx) in item.images" :key="idx">
                <el-image :src="img" fit="contain" style="width: 100%; height: 100%" :preview-src-list="item.images" />
              </el-carousel-item>
            </el-carousel>
            <el-image v-else :src="item.images[0]" fit="contain" style="width: 100%; max-height: 300px" :preview-src-list="item.images" />
          </template>
          <el-empty v-else description="无图片" :image-size="80" />
        </el-col>
        <el-col :span="14">
          <h2 class="item-title">{{ item.title }}</h2>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="分类">{{ item.category }}</el-descriptions-item>
            <el-descriptions-item label="易腐品">
              {{ item.perishable === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="拾取地点" :span="2">{{ item.foundLocation }}</el-descriptions-item>
            <el-descriptions-item label="拾取时间" :span="2">{{ formatTime(item.foundTime) }}</el-descriptions-item>
            <el-descriptions-item label="发布人">
              {{ item.founderName || '匿名' }}
              <el-tag v-if="item.newUser" size="small" type="warning">新手发布</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ formatTime(item.publishedAt) }}</el-descriptions-item>
            <el-descriptions-item label="存放投放点" :span="2">
              {{ item.dropPointName || (item.itemStatus === 6 ? '待发布人交物' : '未指定') }}
            </el-descriptions-item>
            <el-descriptions-item label="物品描述" :span="2">
              {{ item.description || '无描述' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="action-bar" v-if="userStore.isLoggedIn">
            <template v-if="isMyItem">
              <template v-if="item.itemStatus === 6">
                <el-select v-model="selectedDropPointId" placeholder="选择投放点" style="width: 200px">
                  <el-option v-for="p in dropPoints" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-button type="primary" :loading="acting" @click="handleHandIn">已投放</el-button>
              </template>
              <el-button
                v-if="[1, 6].includes(item.itemStatus)"
                type="danger"
                plain
                :loading="acting"
                @click="handleInvalidate"
              >
                作废
              </el-button>
            </template>
            <template v-else-if="item.itemStatus === 1">
              <el-button type="primary" @click="claimDialogVisible = true">这是我的，申请认领</el-button>
            </template>
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
            v-else-if="item.itemStatus === 1"
            title="登录后可申请认领"
            type="info"
            :closable="false"
            style="margin-top: 16px"
          />
        </el-col>
      </el-row>
    </el-card>

    <el-card v-if="item && isMyItem && item.itemStatus !== 0" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>收到的认领申请</span>
          <el-button link type="primary" @click="$router.push('/claims')">全部管理</el-button>
        </div>
      </template>
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
    </el-card>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { foundItemApi, dropPointApi, claimApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ITEM_STATUS_MAP, CLAIM_STATUS_MAP } from '@/types'
import type { FoundItem, DropPoint, ClaimApply } from '@/types'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const acting = ref(false)
const item = ref<FoundItem | null>(null)
const dropPoints = ref<DropPoint[]>([])
const claims = ref<ClaimApply[]>([])
const selectedDropPointId = ref<number | undefined>()

const claimDialogVisible = ref(false)
const claimAnswer = ref('')
const claiming = ref(false)

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectingClaim = ref<ClaimApply | null>(null)

const itemId = computed(() => route.params.id as string)
const isMyItem = computed(() => {
  return !!item.value && userStore.userInfo?.id === item.value.founderId
})

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await foundItemApi.getById(itemId.value)
    item.value = res.data
    if (isMyItem.value) {
      await fetchClaims()
    }
  } catch (error) {
    console.error('加载详情失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchDropPoints = async () => {
  try {
    const res = await dropPointApi.list()
    dropPoints.value = res.data
  } catch (error) {
    console.error('加载投放点失败:', error)
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

const handleHandIn = async () => {
  if (!selectedDropPointId.value) {
    ElMessage.warning('请选择投放点')
    return
  }
  acting.value = true
  try {
    await foundItemApi.handIn(itemId.value, selectedDropPointId.value)
    ElMessage.success('交物成功，招领已公开')
    await fetchDetail()
  } catch (error) {
    console.error('交物失败:', error)
  } finally {
    acting.value = false
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

onMounted(() => {
  fetchDetail()
  fetchDropPoints()
})
</script>

<style scoped>
.detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-title {
  margin: 0 0 16px 0;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.answer-review { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.answer-review small { color: #ad7a26; font-size: 11px; }
</style>
