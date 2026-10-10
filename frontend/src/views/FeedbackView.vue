<template>
  <div class="feedback-container">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="我的举报" name="reports" />
        <el-tab-pane label="我的纠纷申诉" name="disputes" />
        <el-tab-pane label="被下架申诉" name="takedowns" />
        <el-tab-pane label="联系我们" name="contact" />
      </el-tabs>

      <!-- 我的举报 -->
      <div v-if="activeTab === 'reports'" v-loading="reportsLoading">
        <el-table :data="myReports" stripe>
          <el-table-column prop="id" label="编号" width="70" />
          <el-table-column label="举报对象" min-width="160">
            <template #default="{ row }">
              {{ row.targetTitle || ('#' + (row.itemId || row.noticeId)) }}
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ REPORT_TYPE_MAP[row.reportType] || row.reportType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="举报描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="REPORT_STATUS_MAP[row.status]?.type">
                {{ REPORT_STATUS_MAP[row.status]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handlerNote" label="处理结果" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.handlerNote || '—' }}</template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 我的纠纷申诉 -->
      <div v-if="activeTab === 'disputes'" v-loading="disputesLoading">
        <div style="margin-bottom: 12px">
          <el-button type="primary" @click="openDisputeDialog">发起纠纷申诉</el-button>
          <span style="margin-left: 12px; color: #909399; font-size: 13px">
            适用于：领取后发现物品不符、物品被冒领、物品在投放点丢失等情况
          </span>
        </div>
        <el-table :data="myDisputes" stripe>
          <el-table-column prop="id" label="工单号" width="80" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-tag :type="DISPUTE_TYPE_MAP[row.disputeType]?.type">{{ DISPUTE_TYPE_MAP[row.disputeType]?.text || row.disputeType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联物品" min-width="160">
            <template #default="{ row }">{{ row.itemTitle || ('物品 #' + row.itemId) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="DISPUTE_STATUS_MAP[row.status]?.type">{{ DISPUTE_STATUS_MAP[row.status]?.text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handlerNote" label="处理意见" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.handlerNote || '—' }}</template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 被下架申诉 -->
      <div v-if="activeTab === 'takedowns'" v-loading="takedownsLoading">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px"
          title="以下是您发布的、被管理员下架的内容。如认为下架有误，可提交申诉，管理员审核通过后将恢复公开。" />
        <el-table :data="takedownList" stripe>
          <el-table-column prop="id" label="编号" width="70" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row._type === 'item' ? 'primary' : 'warning'">
                {{ row._type === 'item' ? '招领' : '寻物启事' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="takedownReason" label="下架原因" min-width="200" show-overflow-tooltip />
          <el-table-column label="申诉状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="APPEAL_STATUS_MAP[row.appealStatus]?.type || 'info'">
                {{ APPEAL_STATUS_MAP[row.appealStatus]?.text || '未申诉' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.appealStatus || row.appealStatus === 0 || row.appealStatus === 3"
                link type="primary" @click="openAppealDialog(row)">申诉</el-button>
              <span v-else style="color: #909399">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 联系我们 -->
      <div v-if="activeTab === 'contact'">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never" class="contact-card">
              <h3 style="margin-top: 0">系统管理员</h3>
              <p>负责平台规则、积分、内容审核、全局问题处理</p>
              <p><b>邮箱：</b>admin@school.edu.cn</p>
              <p><b>办公时间：</b>工作日 9:00-17:00</p>
              <p style="color: #909399; font-size: 13px">如遇积分异常、账号问题、内容申诉等，请联系系统管理员。</p>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="contact-card">
              <h3 style="margin-top: 0">投放点管理员</h3>
              <p>负责各投放点的实物清点、保管与积分发放</p>
              <el-table :data="pointAdmins" size="small" style="margin-top: 8px">
                <el-table-column prop="name" label="投放点" />
                <el-table-column prop="location" label="位置" />
                <el-table-column prop="adminName" label="管理员" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" style="margin-top: 16px">
          <h3 style="margin-top: 0">反馈渠道说明</h3>
          <ul style="line-height: 1.8; color: #606266">
            <li><b>举报：</b>在招领/启事详情页点击「举报」按钮，选择类型并填写描述</li>
            <li><b>纠纷申诉：</b>在「我的纠纷申诉」tab 发起，适用于领取后物品不符、被冒领等</li>
            <li><b>下架申诉：</b>在「被下架申诉」tab 对被下架的内容提交申诉</li>
            <li><b>物品丢失：</b>发现物品在投放点丢失，直接在物品详情页提交丢失申诉工单</li>
          </ul>
        </el-card>
      </div>
    </el-card>

    <!-- 纠纷申诉对话框 -->
    <el-dialog v-model="disputeDialogVisible" title="发起纠纷申诉" width="560px">
      <el-form :model="disputeForm" label-width="90px">
        <el-form-item label="申诉类型" required>
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio value="ITEM_MISMATCH">物品不符/损坏</el-radio>
            <el-radio value="FALSE_CLAIM">物品被冒领</el-radio>
            <el-radio value="OTHER">其他纠纷</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="disputeForm.disputeType === 'FALSE_CLAIM'" label="选择物品" required>
          <el-select v-model="disputeForm.itemId" placeholder="选择被冒领的物品" style="width: 100%" filterable>
            <el-option v-for="i in claimedItems" :key="i.id" :value="i.id" :label="`#${i.id} ${i.title}`" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择已完成领取的认领订单" style="width: 100%">
            <el-option v-for="a in completedClaims" :key="a.id" :value="a.id"
              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉描述" required>
          <el-input v-model="disputeForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispute">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- 下架申诉对话框 -->
    <el-dialog v-model="appealDialogVisible" title="申诉下架" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        :title="`正在申诉：${currentAppeal?.title}`" />
      <el-form label-width="80px">
        <el-form-item label="申诉理由" required>
          <el-input v-model="appealReason" type="textarea" :rows="4" maxlength="500" show-word-limit
            placeholder="说明为什么认为该内容不应被下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi, claimApi, foundItemApi, reportApi, moderationApi } from '@/api'
import type { DisputeVO, ClaimApply, FoundItem } from '@/types'
import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP } from '@/types'

const activeTab = ref('reports')
const formatTime = (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '—')

const REPORT_TYPE_MAP: Record<string, string> = {
  FAKE_PUBLISH: '虚假发布',
  DESC_MISMATCH: '描述不符',
  OTHER: '其他'
}
const REPORT_STATUS_MAP: Record<number, { text: string; type: string }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '举报成立', type: 'success' },
  2: { text: '举报不成立', type: 'info' }
}
const APPEAL_STATUS_MAP: Record<number, { text: string; type: string }> = {
  0: { text: '未申诉', type: 'info' },
  1: { text: '审核中', type: 'warning' },
  2: { text: '申诉通过', type: 'success' },
  3: { text: '申诉驳回', type: 'danger' }
}

// 我的举报
const myReports = ref<any[]>([])
const reportsLoading = ref(false)
const loadReports = async () => {
  reportsLoading.value = true
  try {
    const res = await reportApi.listMyReports({ page: 1, size: 50 })
    myReports.value = res.data.records
  } finally {
    reportsLoading.value = false
  }
}

// 纠纷申诉
const myDisputes = ref<DisputeVO[]>([])
const disputesLoading = ref(false)
const loadDisputes = async () => {
  disputesLoading.value = true
  try {
    const res = await disputeApi.listMy({ page: 1, size: 50 })
    myDisputes.value = res.data.records
  } finally {
    disputesLoading.value = false
  }
}
const disputeDialogVisible = ref(false)
const submitting = ref(false)
const completedClaims = ref<ClaimApply[]>([])
const claimedItems = ref<FoundItem[]>([])
const disputeForm = ref({ disputeType: 'ITEM_MISMATCH', applyId: undefined as number | undefined, itemId: undefined as number | undefined, description: '' })

const openDisputeDialog = async () => {
  const [claimRes, itemRes] = await Promise.all([
    claimApi.listMy({ page: 1, size: 100 }),
    foundItemApi.list({ page: 1, size: 100, itemStatus: 3 })
  ])
  completedClaims.value = claimRes.data.records.filter((a) => a.applyStatus === 1)
  claimedItems.value = itemRes.data.records
  disputeForm.value = { disputeType: 'ITEM_MISMATCH', applyId: undefined, itemId: undefined, description: '' }
  disputeDialogVisible.value = true
}

const submitDispute = async () => {
  const f = disputeForm.value
  if (!f.description.trim()) { ElMessage.warning('请填写申诉描述'); return }
  if (f.disputeType === 'FALSE_CLAIM' && !f.itemId) { ElMessage.warning('请选择物品'); return }
  if (f.disputeType !== 'FALSE_CLAIM' && !f.applyId) { ElMessage.warning('请选择认领订单'); return }
  submitting.value = true
  try {
    await disputeApi.create({
      disputeType: f.disputeType,
      applyId: f.disputeType === 'FALSE_CLAIM' ? undefined : f.applyId,
      itemId: f.disputeType === 'FALSE_CLAIM' ? f.itemId : undefined,
      description: f.description.trim(),
      evidenceImages: []
    })
    ElMessage.success('申诉已提交')
    disputeDialogVisible.value = false
    loadDisputes()
  } finally { submitting.value = false }
}

// 被下架申诉
const takedownList = ref<any[]>([])
const takedownsLoading = ref(false)
const loadTakedowns = async () => {
  takedownsLoading.value = true
  try {
    const res = await moderationApi.myTakedowns()
    const items = (res.data.items || []).map((i: any) => ({ ...i, _type: 'item' }))
    const notices = (res.data.notices || []).map((n: any) => ({ ...n, _type: 'notice' }))
    takedownList.value = [...items, ...notices].sort((a, b) =>
      new Date(b.takedownAt).getTime() - new Date(a.takedownAt).getTime())
  } finally {
    takedownsLoading.value = false
  }
}

const appealDialogVisible = ref(false)
const currentAppeal = ref<any>(null)
const appealReason = ref('')
const openAppealDialog = (row: any) => {
  currentAppeal.value = row
  appealReason.value = ''
  appealDialogVisible.value = true
}
const submitAppeal = async () => {
  if (!appealReason.value.trim()) { ElMessage.warning('请填写申诉理由'); return }
  submitting.value = true
  try {
    if (currentAppeal.value._type === 'item') {
      await moderationApi.appealItem(currentAppeal.value.id, appealReason.value.trim())
    } else {
      await moderationApi.appealNotice(currentAppeal.value.id, appealReason.value.trim())
    }
    ElMessage.success('申诉已提交')
    appealDialogVisible.value = false
    loadTakedowns()
  } finally { submitting.value = false }
}

// 联系我们 - 点位管理员
const pointAdmins = ref<any[]>([])
const loadContact = async () => {
  try {
    const res = await fetch('/api/drop-points', { headers: { Authorization: 'Bearer ' + localStorage.getItem('token') } })
    const json = await res.json()
    pointAdmins.value = (json.data || []).map((p: any) => ({
      name: p.name,
      location: p.location || p.area || '—',
      adminName: p.adminName || p.adminRealName || '—'
    }))
  } catch { /* ignore */ }
}

const onTabChange = (tab: string) => {
  if (tab === 'reports') loadReports()
  else if (tab === 'disputes') loadDisputes()
  else if (tab === 'takedowns') loadTakedowns()
  else if (tab === 'contact') loadContact()
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.feedback-container { max-width: 1200px; margin: 0 auto; }
.contact-card { height: 100%; }
</style>
