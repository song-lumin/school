<template>
  <div class="dispute-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的申诉工单</span>
          <div>
            <el-button type="primary" @click="openDisputeDialog">纠纷申诉</el-button>
          </div>
        </div>
      </template>

      <el-table :data="disputes" v-loading="loading" stripe>
        <el-table-column prop="id" label="工单号" width="80" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="DISPUTE_TYPE_MAP[row.disputeType]?.type || 'info'">
              {{ DISPUTE_TYPE_MAP[row.disputeType]?.text || row.disputeType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联物品" min-width="160">
          <template #default="{ row }">
            {{ row.itemTitle || ('物品 #' + row.itemId) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="申诉描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="DISPUTE_STATUS_MAP[row.status]?.type">
              {{ DISPUTE_STATUS_MAP[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理意见" min-width="160">
          <template #default="{ row }">
            {{ row.handlerNote || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button link type="warning" @click="openChat(row)">沟通</el-button>
          </template>
        </el-table-column>
      </el-table>

      <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 站点与系统管理员" />

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="loadDisputes"
      />
    </el-card>

    <!-- 纠纷申诉对话框 -->
    <el-dialog v-model="disputeDialogVisible" title="发起纠纷申诉" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="alertText"
        style="margin-bottom: 16px"
      />
      <el-form :model="disputeForm" label-width="90px">
        <el-form-item label="申诉类型" required>
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio value="ITEM_MISMATCH">物品不符</el-radio>
            <el-radio value="ITEM_DAMAGED">物品损坏</el-radio>
            <el-radio value="ITEM_NOT_FOUND">现场找不到物品</el-radio>
            <el-radio value="FALSE_CLAIM">物品被冒领</el-radio>
            <el-radio value="OTHER">其他纠纷</el-radio>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; line-height: 1.6; margin-top: 6px">
            「找不到物品」用于<b>已通过审核、尚未取走</b>的认领：到站货架没有实物，由投放点核查货架与监控；
            「不符 / 损坏」是取走之后才发现的问题，定责后物品转「待归还」。
          </div>
        </el-form-item>
        <el-form-item v-if="disputeForm.disputeType === 'FALSE_CLAIM'" label="选择物品" required>
          <el-select v-model="disputeForm.itemId" placeholder="选择被冒领的物品" style="width: 100%" filterable>
            <el-option
              v-for="i in claimedItems"
              :key="i.id"
              :value="i.id"
              :label="`#${i.id} ${i.title}`"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else-if="disputeForm.disputeType === 'ITEM_NOT_FOUND'" label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择「已通过、待取件」的认领订单" style="width: 100%">
            <el-option
              v-for="a in pendingPickupClaims"
              :key="a.id"
              :value="a.id"
              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}（待取件）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择已完成领取的认领订单" style="width: 100%">
            <el-option
              v-for="a in completedClaims"
              :key="a.id"
              :value="a.id"
              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}（领取于 ${formatTime(a.pickupTime ?? undefined)}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉描述" required>
          <el-input
            v-model="disputeForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="描述遇到的问题"
          />
        </el-form-item>
        <el-form-item label="证据图片">
          <EvidenceUpload v-model="disputeForm.evidenceImages" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispute">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="600px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="工单号">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ DISPUTE_TYPE_MAP[current.disputeType]?.text }}
        </el-descriptions-item>
        <el-descriptions-item label="关联物品" :span="2">
          {{ current.itemTitle || ('物品 #' + current.itemId) }}
        </el-descriptions-item>
        <el-descriptions-item label="申诉描述" :span="2">{{ current.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="DISPUTE_STATUS_MAP[current.status]?.type">
            {{ DISPUTE_STATUS_MAP[current.status]?.text }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="处理意见" :span="2">{{ current.handlerNote || '待处理' }}</el-descriptions-item>
        <el-descriptions-item label="证据图片" :span="2">
          <template v-if="current.evidenceImages && current.evidenceImages.length">
            <el-image
              v-for="(img, idx) in current.evidenceImages"
              :key="idx"
              :src="img"
              :preview-src-list="current.evidenceImages"
              fit="cover"
              style="width: 80px; height: 80px; margin-right: 8px"
            />
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi, claimApi, foundItemApi } from '@/api'
import EvidenceUpload from '@/components/EvidenceUpload.vue'
import ChatDialog from '@/components/ChatDialog.vue'
import type { DisputeVO, ClaimApply, FoundItem } from '@/types'
import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP } from '@/types'

const disputes = ref<DisputeVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const submitting = ref(false)

const myClaims = ref<ClaimApply[]>([])
const completedClaims = computed(() => myClaims.value.filter((a) => a.applyStatus === 1))
const pendingPickupClaims = computed(() => myClaims.value.filter((a) => a.applyStatus === 4))
const claimedItems = ref<FoundItem[]>([])

const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const disputeDialogVisible = ref(false)
const detailVisible = ref(false)
const current = ref<DisputeVO | null>(null)

const disputeForm = ref({
  disputeType: 'ITEM_MISMATCH',
  applyId: undefined as number | undefined,
  itemId: undefined as number | undefined,
  description: '',
  evidenceImages: [] as string[]
})

const alertText = computed(() => {
  const t = disputeForm.value.disputeType
  if (t === 'FALSE_CLAIM') {
    return '如果你是物品的真正失主，发现物品被他人冒领，可选择物品发起冒领申诉，由管理员审核裁定（无时间限制）'
  }
  if (t === 'ITEM_NOT_FOUND') {
    return '仅限「已通过、待取件」的订单：到站货架上没有实物时提交，系统会同时通知投放点负责人核查货架与监控；核查结论由系统管理员定责'
  }
  return '仅限认领人在领取后 7 天内提交，针对已完成领取的订单'
})

const formatTime = (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '—')

const loadDisputes = async () => {
  loading.value = true
  try {
    const res = await disputeApi.listMy({ page: page.value, size: size.value })
    disputes.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openDisputeDialog = async () => {
  const [claimRes, itemRes] = await Promise.all([
    claimApi.listMy({ page: 1, size: 100 }),
    foundItemApi.list({ page: 1, size: 100, itemStatus: 3 })
  ])
  myClaims.value = claimRes.data.records
  claimedItems.value = itemRes.data.records
  disputeForm.value = {
    disputeType: 'ITEM_MISMATCH',
    applyId: undefined,
    itemId: undefined,
    description: '',
    evidenceImages: []
  }
  disputeDialogVisible.value = true
}

const submitDispute = async () => {
  const form = disputeForm.value
  if (!form.description.trim()) {
    ElMessage.warning('请填写申诉描述')
    return
  }
  if (form.disputeType === 'FALSE_CLAIM') {
    if (!form.itemId) {
      ElMessage.warning('请选择被冒领的物品')
      return
    }
  } else if (!form.applyId) {
    ElMessage.warning('请选择认领订单')
    return
  }
  submitting.value = true
  try {
    await disputeApi.create({
      disputeType: form.disputeType,
      applyId: form.disputeType === 'FALSE_CLAIM' ? undefined : form.applyId,
      itemId: form.disputeType === 'FALSE_CLAIM' ? form.itemId : undefined,
      description: form.description.trim(),
      evidenceImages: form.evidenceImages
    })
    ElMessage.success('申诉已提交，请等待管理员处理')
    disputeDialogVisible.value = false
    page.value = 1
    await loadDisputes()
  } finally {
    submitting.value = false
  }
}

const showDetail = (row: DisputeVO) => {
  current.value = row
  detailVisible.value = true
}

onMounted(loadDisputes)
</script>

<style scoped>
.dispute-container {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
