<template>
  <div class="claim-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button value="my">我的认领申请</el-radio-button>
            <el-radio-button value="received">收到的申请</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 我的认领申请：失主视角 -->
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
          <el-table-column label="置信度" width="90">
            <template #default="{ row }">
              <span v-if="row.confidenceScore != null">
                <el-tag size="small" :type="row.lowConfidence === 1 ? 'danger' : 'success'">
                  {{ row.confidenceScore }}分
                </el-tag>
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="applyStatus" label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="CLAIM_STATUS_MAP[row.applyStatus]?.type || 'info'">
                {{ CLAIM_STATUS_MAP[row.applyStatus]?.text || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <!-- 已通过待取件：失主到现场后自己操作 -->
              <template v-if="row.applyStatus === 4">
                <el-alert type="warning" :closable="false" style="margin-bottom: 8px">
                  请前往投放点自行找物核对：找到本人物品→上传签字合影确认领取；不是本人→取消认领
                </el-alert>
                <el-button size="small" type="success" :disabled="acting" @click="openPickupDialog(row)">
                  现场确认领取（签字合影）
                </el-button>
                <el-button size="small" type="danger" plain :disabled="acting" @click="handleCancelPickup(row)">
                  不是我的，取消认领
                </el-button>
              </template>
              <el-alert v-else-if="row.applyStatus === 1" type="info" :closable="false" style="margin-bottom: 6px">
                已领取成功。自领取日起 7 天内为保障期：如有物品不符、调监控核对等需求可申诉；
                <span v-if="warrantyDeadline(row)">保障截止 {{ warrantyDeadline(row) }}，逾期将无法再申请调取监控或系统申诉。</span>
              </el-alert>
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

      <!-- 收到的申请：发布者视角 -->
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
                  <!-- 待投放状态：发布者点"已投放"后公开 -->
                  <el-button
                    v-if="it.itemStatus === 6"
                    size="small"
                    type="warning"
                    style="margin-left: 12px"
                    @click.stop="handleMarkPlaced(it)"
                  >
                    我已投放实物，点此公开
                  </el-button>
                  <el-badge :value="receivedByItem[it.id]?.length || 0" type="primary" style="margin-left: 8px">
                    <span class="collapse-count">申请 {{ receivedByItem[it.id]?.length || 0 }} 条</span>
                  </el-badge>
                </div>
              </template>

              <!-- 待投放状态提示 -->
              <el-alert
                v-if="it.itemStatus === 6"
                type="info"
                :closable="false"
                title="此招领暂未公开。请将实物放入所选投放点存放区后，点击上方'我已投放实物'按钮。"
                style="margin-bottom: 10px"
              />

              <el-empty
                description="该物品暂无认领申请"
                v-if="!receivedByItem[it.id] || receivedByItem[it.id].length === 0"
                :image-size="50"
              />
              <el-table v-else :data="receivedByItem[it.id]" size="small">
                <el-table-column prop="claimerName" label="申请人" width="110" />
                <el-table-column label="置信度" width="90">
                  <template #default="{ row }">
                    <span v-if="row.confidenceScore != null">
                      <el-tag size="small" :type="row.lowConfidence === 1 ? 'danger' : 'success'">
                        {{ row.confidenceScore }}分
                      </el-tag>
                    </span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="答案与审核提示" min-width="220">
                  <template #default="{ row }">
                    <div class="answer-review">
                      <span>{{ row.answer }}</span>
                      <small v-if="row.confidenceReason">{{ row.confidenceReason }}</small>
                    </div>
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
                <el-table-column prop="applyStatus" label="状态" width="100">
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
                    <el-alert v-else-if="row.applyStatus === 4" type="info" :closable="false" title="失主前往投放点领取中" />
                    <span v-else class="no-action">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </template>
      </template>

    </el-card>



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
import { ElMessage, ElMessageBox } from 'element-plus'
import { claimApi, foundItemApi } from '@/api'
import { CLAIM_STATUS_MAP, ITEM_STATUS_MAP } from '@/types'
import type { ClaimApply, FoundItem } from '@/types'
import PickupDialog from '@/components/PickupDialog.vue'

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


const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const warrantyDeadline = (row: ClaimApply): string => {
  const t = (row as any).pickupTime || (row as any).updatedAt || row.createdAt
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  d.setDate(d.getDate() + 7)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
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
  if (tab === 'received') {
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
    ElMessage.success('已同意，失主将看到投放点位置并前往领取')
    refreshCurrent()
  } catch (error) {
    console.error('同意失败:', error)
  } finally {
    acting.value = false
  }
}

const openRejectDialog = (row: ClaimApply) => {
  ElMessageBox.confirm('确定驳回该认领申请吗？', '驳回认领', { type: 'warning', confirmButtonText: '确认驳回', cancelButtonText: '取消' })
    .then(async () => {
      acting.value = true
      try {
        await claimApi.reject(row.id, {})
        ElMessage.success('已驳回')
        refreshCurrent()
      } catch (error) {
        console.error('驳回失败:', error)
      } finally {
        acting.value = false
      }
    })
    .catch(() => {})
}

const handleMarkPlaced = async (item: FoundItem) => {
  try {
    await ElMessageBox.confirm(
      '请确认已将实物放入所选投放点存放区。确认后招领将公开对外展示。',
      '标记已投放',
      { confirmButtonText: '已投放，公开', cancelButtonText: '还没投放', type: 'warning' }
    )
  } catch {
    return
  }
  acting.value = true
  try {
    await foundItemApi.markPlaced(item.id)
    ElMessage.success('招领已公开，等待失主认领')
    fetchReceived()
  } catch (error) {
    console.error('标记已投放失败:', error)
  } finally {
    acting.value = false
  }
}

const handleCancelPickup = async (row: ClaimApply) => {
  try {
    await ElMessageBox.confirm(
      '现场核对实物不是您的物品？取消认领后，其他被锁定的申请将解锁恢复。',
      '取消认领',
      { confirmButtonText: '不是我的，取消', cancelButtonText: '再看看', type: 'warning' }
    )
  } catch {
    return
  }
  acting.value = true
  try {
    await claimApi.cancelPickup(row.id)
    ElMessage.success('已取消认领，其他申请已解锁')
    fetchMyClaims()
  } catch (error) {
    console.error('取消认领失败:', error)
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
.answer-review small { color: #909399; font-size: 11px; }
</style>
