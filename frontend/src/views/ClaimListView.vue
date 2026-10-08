<template>
  <div class="claim-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button value="my">我的认领申请</el-radio-button>
            <el-radio-button value="received">收到的申请</el-radio-button>
            <el-radio-button v-if="userStore.isAdmin || userStore.isPointAdmin" value="review">待审核处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <template v-if="activeTab === 'my'">
        <el-table :data="claims" v-loading="loading" style="width: 100%">
          <el-table-column prop="itemTitle" label="物品" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/items/${row.itemId}`)">
                {{ row.itemTitle }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="answer" label="我的答案" min-width="180" show-overflow-tooltip />
          <el-table-column prop="applyStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="CLAIM_STATUS_MAP[row.applyStatus]?.type || 'info'">
                {{ CLAIM_STATUS_MAP[row.applyStatus]?.text || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rejectReason" label="驳回原因" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <span v-if="row.applyStatus === 4" class="pickup-tip">审核已通过，请前往投放点领取（现场需合影并签字确认）</span>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="myPage"
            v-model:page-size="pageSize"
            :total="myTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchMyClaims"
            @current-change="fetchMyClaims"
          />
        </div>
      </template>

      <template v-else>
        <el-empty
          description="您还没有发布过招领物品"
          v-if="!loading && myItems.length === 0"
        />
        <template v-else>
          <el-collapse v-loading="loading">
            <el-collapse-item v-for="it in myItems" :key="it.id">
              <template #title>
                <div class="collapse-title">
                  <el-link type="primary" @click.stop="$router.push(`/items/${it.id}`)">{{ it.title }}</el-link>
                  <el-tag size="small" :type="ITEM_STATUS_MAP[it.itemStatus]?.type || 'info'" style="margin-left: 8px">
                    {{ ITEM_STATUS_MAP[it.itemStatus]?.text || '未知' }}
                  </el-tag>
                  <el-badge :value="receivedByItem[it.id]?.length || 0" type="primary" style="margin-left: 8px">
                    <span class="collapse-count">申请 {{ receivedByItem[it.id]?.length || 0 }} 条</span>
                  </el-badge>
                </div>
              </template>
              <el-empty
                description="该物品暂无认领申请"
                v-if="!receivedByItem[it.id] || receivedByItem[it.id].length === 0"
                :image-size="50"
              />
              <el-table v-else :data="receivedByItem[it.id]" size="small">
                <el-table-column prop="claimerName" label="申请人" width="110" />
                <el-table-column label="答案与审核提示" min-width="220">
                  <template #default="{ row }">
                    <div class="answer-review"><span>{{ row.answer }}</span><el-tag v-if="row.lowConfidence === 1" size="small" type="warning">低置信度</el-tag><small v-if="row.lowConfidence === 1">{{ row.confidenceReason }}</small></div>
                  </template>
                </el-table-column>
                <el-table-column label="来源" width="110">
                  <template #default="{ row }">
                    <el-link v-if="row.sourceNoticeId" type="primary" @click="$router.push(`/notices/${row.sourceNoticeId}`)">寻物启事 #{{ row.sourceNoticeId }}</el-link>
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
                      <el-button link type="success" :disabled="acting" @click="handleApprove(row)">同意</el-button>
                      <el-button link type="danger" :disabled="acting" @click="openRejectDialog(row)">驳回</el-button>
                    </template>
                    <template v-else-if="row.applyStatus === 4">
                      <el-button link type="primary" :disabled="acting" @click="openPickupDialog(row)">确认取件</el-button>
                    </template>
                    <span v-else class="no-action">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </template>
      </template>

      <template v-if="activeTab === 'review'">
        <div class="review-filter">
          <el-radio-group v-model="reviewStatus" @change="fetchReview">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="4">待取件</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="reviewClaims" v-loading="loading" style="width: 100%">
          <el-table-column prop="itemTitle" label="物品" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/items/${row.itemId}`)">
                {{ row.itemTitle }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="claimerName" label="申请人" width="110" />
          <el-table-column label="答案与审核提示" min-width="220">
            <template #default="{ row }">
              <div class="answer-review"><span>{{ row.answer }}</span><el-tag v-if="row.lowConfidence === 1" size="small" type="warning">低置信度</el-tag><small v-if="row.lowConfidence === 1">{{ row.confidenceReason }}</small></div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="110">
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
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <template v-if="row.applyStatus === 0">
                <el-button link type="success" :disabled="acting" @click="handleApprove(row)">同意</el-button>
                <el-button link type="danger" :disabled="acting" @click="openRejectDialog(row)">驳回</el-button>
              </template>
              <template v-else-if="row.applyStatus === 4">
                <el-button link type="primary" :disabled="acting" @click="openPickupDialog(row)">确认取件</el-button>
              </template>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="reviewPage"
            v-model:page-size="pageSize"
            :total="reviewTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchReview"
            @current-change="fetchReview"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="驳回认领" width="450px">
      <el-input v-model="rejectReason" type="textarea" :rows="2" placeholder="请输入驳回原因" maxlength="200" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <PickupDialog
      ref="pickupDialogRef"
      v-model:visible="pickupDialogVisible"
      :claim-id="pickupClaim?.id ?? null"
      @success="refreshCurrent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { claimApi, foundItemApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { CLAIM_STATUS_MAP, ITEM_STATUS_MAP } from '@/types'
import type { ClaimApply, FoundItem } from '@/types'
import PickupDialog from '@/components/PickupDialog.vue'

const userStore = useUserStore()
const loading = ref(false)
const acting = ref(false)
const activeTab = ref<'my' | 'received' | 'review'>('my')

const claims = ref<ClaimApply[]>([])
const myTotal = ref(0)
const myPage = ref(1)
const pageSize = ref(10)

const myItems = ref<FoundItem[]>([])
const receivedByItem = ref<Record<number, ClaimApply[]>>({})

const reviewClaims = ref<ClaimApply[]>([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewStatus = ref(-1)

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectingClaim = ref<ClaimApply | null>(null)

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchMyClaims = async () => {
  loading.value = true
  try {
    const res = await claimApi.listMy({ page: myPage.value, size: pageSize.value })
    claims.value = res.data.records
    myTotal.value = Number(res.data.total)
  } catch (error) {
    console.error('加载我的申请失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchReceived = async () => {
  loading.value = true
  try {
    const itemsRes = await foundItemApi.listMy({ page: 1, size: 100 })
    myItems.value = itemsRes.data.records
    receivedByItem.value = {}
    await Promise.all(
      myItems.value.map(async (it) => {
        try {
          const res = await claimApi.listByItem(it.id, { page: 1, size: 50 })
          receivedByItem.value[it.id] = res.data.records
        } catch {
          receivedByItem.value[it.id] = []
        }
      })
    )
  } catch (error) {
    console.error('加载收到的申请失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchReview = async () => {
  loading.value = true
  try {
    const params: { status?: number; page: number; size: number } = {
      page: reviewPage.value,
      size: pageSize.value
    }
    if (reviewStatus.value !== -1) {
      params.status = reviewStatus.value
    }
    const res = await claimApi.listForReview(params)
    reviewClaims.value = res.data.records
    reviewTotal.value = Number(res.data.total)
  } catch (error) {
    console.error('加载待审核认领失败:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab: string | number | boolean | undefined) => {
  if (tab === 'received' && myItems.value.length === 0) {
    fetchReceived()
  }
  if (tab === 'review' && reviewClaims.value.length === 0) {
    fetchReview()
  }
}

const refreshCurrent = () => {
  if (activeTab.value === 'my') {
    fetchMyClaims()
  } else if (activeTab.value === 'received') {
    fetchReceived()
  } else {
    fetchReview()
  }
}

const handleApprove = async (row: ClaimApply) => {
  acting.value = true
  try {
    await claimApi.approve(row.id)
    ElMessage.success('已同意，请失主到投放点领取')
    refreshCurrent()
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
    refreshCurrent()
  } catch (error) {
    console.error('驳回失败:', error)
  } finally {
    acting.value = false
  }
}

const pickupDialogVisible = ref(false)
const pickupClaim = ref<ClaimApply | null>(null)
const pickupDialogRef = ref<InstanceType<typeof PickupDialog> | null>(null)

const openPickupDialog = (row: ClaimApply) => {
  pickupClaim.value = row
  pickupDialogVisible.value = true
  pickupDialogRef.value?.init()
}

onMounted(fetchMyClaims)
</script>

<style scoped>
.claim-container {
  max-width: 1300px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-filter {
  margin-bottom: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.no-action {
  color: #c0c4cc;
}

.pickup-tip {
  color: #e6a23c;
  font-size: 12px;
}

.collapse-title {
  display: flex;
  align-items: center;
}

.collapse-count {
  font-size: 13px;
  color: #606266;
}

.answer-review { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.answer-review > span { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.answer-review small { color: #ad7a26; font-size: 11px; }
</style>
