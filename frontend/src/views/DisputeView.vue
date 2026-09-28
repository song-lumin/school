<template>
  <div class="dispute-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的申诉工单</span>
          <div>
            <el-button type="warning" @click="openLostReportDialog">物品丢失申诉</el-button>
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
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

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
        title="仅限认领人在领取后 24 小时内提交，针对已完成领取的订单"
        style="margin-bottom: 16px"
      />
      <el-form :model="disputeForm" label-width="90px">
        <el-form-item label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择已完成领取的认领订单" style="width: 100%">
            <el-option
              v-for="a in completedClaims"
              :key="a.id"
              :value="a.id"
              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}（领取于 ${formatTime(a.pickupTime ?? undefined)}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉类型" required>
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio value="ITEM_MISMATCH">物品不符</el-radio>
            <el-radio value="OTHER">其他纠纷</el-radio>
          </el-radio-group>
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
          <el-input
            v-model="evidenceInput"
            placeholder="输入图片 URL（可选），每行一个"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispute">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 物品丢失申诉对话框 -->
    <el-dialog v-model="lostReportDialogVisible" title="物品丢失申诉" width="560px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅限发布者针对已交至站点（公开中）的物品提交，系统将自动生成监控调取记录"
        style="margin-bottom: 16px"
      />
      <el-form :model="lostForm" label-width="90px">
        <el-form-item label="选择物品" required>
          <el-select v-model="lostForm.itemId" placeholder="选择我发布的公开物品" style="width: 100%">
            <el-option
              v-for="i in myPublicItems"
              :key="i.id"
              :value="i.id"
              :label="`#${i.id} ${i.title}（${i.dropPointName || '站点'}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉描述" required>
          <el-input
            v-model="lostForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="描述物品丢失情况，如最后一次见到物品的时间和地点"
          />
        </el-form-item>
        <el-form-item label="证据图片">
          <el-input
            v-model="evidenceInput"
            placeholder="输入图片 URL（可选），每行一个"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lostReportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLostReport">提交申诉</el-button>
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi, claimApi, foundItemApi } from '@/api'
import type { DisputeVO, ClaimApply, FoundItem } from '@/types'
import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP } from '@/types'

const disputes = ref<DisputeVO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const submitting = ref(false)

const completedClaims = ref<ClaimApply[]>([])
const myPublicItems = ref<FoundItem[]>([])

const disputeDialogVisible = ref(false)
const lostReportDialogVisible = ref(false)
const detailVisible = ref(false)
const current = ref<DisputeVO | null>(null)

const disputeForm = ref({ applyId: undefined as number | undefined, disputeType: 'ITEM_MISMATCH', description: '' })
const lostForm = ref({ itemId: undefined as number | undefined, description: '' })
const evidenceInput = ref('')

const formatTime = (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '—')

const parseEvidence = (): string[] => {
  return evidenceInput.value
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
}

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
  const res = await claimApi.listMy({ page: 1, size: 100 })
  completedClaims.value = res.data.records.filter((a) => a.applyStatus === 1)
  disputeForm.value = { applyId: undefined, disputeType: 'ITEM_MISMATCH', description: '' }
  evidenceInput.value = ''
  disputeDialogVisible.value = true
}

const openLostReportDialog = async () => {
  const res = await foundItemApi.listMy({ page: 1, size: 100, itemStatus: 1 })
  myPublicItems.value = res.data.records
  lostForm.value = { itemId: undefined, description: '' }
  evidenceInput.value = ''
  lostReportDialogVisible.value = true
}

const submitDispute = async () => {
  if (!disputeForm.value.applyId) {
    ElMessage.warning('请选择认领订单')
    return
  }
  if (!disputeForm.value.description.trim()) {
    ElMessage.warning('请填写申诉描述')
    return
  }
  submitting.value = true
  try {
    await disputeApi.create({
      disputeType: disputeForm.value.disputeType,
      applyId: disputeForm.value.applyId,
      description: disputeForm.value.description.trim(),
      evidenceImages: parseEvidence()
    })
    ElMessage.success('申诉已提交，请等待管理员处理')
    disputeDialogVisible.value = false
    page.value = 1
    await loadDisputes()
  } finally {
    submitting.value = false
  }
}

const submitLostReport = async () => {
  if (!lostForm.value.itemId) {
    ElMessage.warning('请选择物品')
    return
  }
  if (!lostForm.value.description.trim()) {
    ElMessage.warning('请填写申诉描述')
    return
  }
  submitting.value = true
  try {
    await disputeApi.createLostReport({
      itemId: lostForm.value.itemId,
      description: lostForm.value.description.trim(),
      evidenceImages: parseEvidence()
    })
    ElMessage.success('丢失申诉已提交，系统已生成监控调取记录')
    lostReportDialogVisible.value = false
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
