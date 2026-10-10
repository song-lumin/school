<template>
  <div class="point-admin-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>投放点管理</span>
          <el-select v-model="selectedPointId" placeholder="选择站点" style="width: 260px" @change="fetchAll">
            <el-option v-for="p in myPoints" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </template>

      <el-row :gutter="12" class="stat-row">
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#909399">{{ stats.pending }}</div><div class="stat-label">待投放</div><div class="stat-sub">发布未点已投放</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#409eff">{{ stats.placed }}</div><div class="stat-label">已投放</div><div class="stat-sub">含在站+已领待补</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#e6a23c">{{ stats.picked }}</div><div class="stat-label">已领取</div><div class="stat-sub">失主已取走实物</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card stat-physical"><div class="stat-value" style="color:#67c23a">{{ stats.onSite }}</div><div class="stat-label">在站实物数</div><div class="stat-sub">= 已投放 - 已领取</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#e6a23c">{{ stats.checking }}</div><div class="stat-label">待核对</div><div class="stat-sub">hand_in_status=1</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#67c23a">{{ stats.checked }}</div><div class="stat-label">已核对</div><div class="stat-sub">已发/待补发积分</div></el-card></el-col>
      </el-row>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="在站管理清单" name="active">
          <div class="filter-bar">
            <el-input v-model="filter.keyword" placeholder="搜索物品名/描述/位置" style="width:240px" clearable />
            <el-select v-model="filter.category" placeholder="分类" clearable style="width:130px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="filter.biz" placeholder="业务状态" clearable style="width:170px">
              <el-option label="待投放" value="pending" />
              <el-option label="已投放" value="placed" />
              <el-option label="已领取(保障期内)" value="picked" />
              <el-option label="待核对" value="checking" />
              <el-option label="已核对" value="checked" />
            </el-select>
            <el-button @click="resetFilter">重置</el-button>
          </div>
          <el-alert type="info" :closable="false" style="margin:12px 0"
            title="巡检要点：对照本清单清点实物。若同一实物对应多条招领帖（重复/虚假投放），选中可疑帖点'管理员下架'并填写原因；失主已先领取的，查看领取凭证后点'核对补发'。" />
          <el-table :data="activeList" v-loading="loading" stripe>
            <el-table-column prop="item.id" label="ID" width="70" />
            <el-table-column label="图片" width="80">
              <template #default="{ row }">
                <el-image v-if="row.item.images && row.item.images.length" :src="row.item.images[0]" :preview-src-list="row.item.images" fit="cover" style="width:48px;height:48px;border-radius:4px" preview-teleported />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="物品" min-width="200">
              <template #default="{ row }">
                <router-link :to="`/items/${row.item.id}`" class="item-link">{{ row.item.title }}</router-link>
                <div class="sub-text">{{ row.item.foundLocation }} · {{ formatTime(row.item.publishedAt) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100"><template #default="{ row }"><el-tag size="small">{{ row.item.category || '-' }}</el-tag></template></el-table-column>
            <el-table-column label="业务状态" width="130"><template #default="{ row }"><el-tag size="small" :type="bizTag(row).type">{{ bizTag(row).text }}</el-tag></template></el-table-column>
            <el-table-column label="核对状态" width="130"><template #default="{ row }"><el-tag size="small" :type="checkTag(row).type">{{ checkTag(row).text }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.handInStatus === 1" type="primary" size="small" @click="openCheckDialog(row)">{{ row.item.itemStatus === 3 ? '核对补发' : '核对发分' }}</el-button>
                <el-button v-if="row.item.itemStatus !== 3 && row.item.itemStatus !== 5 && row.item.itemStatus !== 0" type="danger" size="small" plain @click="openTakedownDialog(row)">管理员下架</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="在站暂无物品" /></template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="checkDialogVisible" title="巡检核对" width="440px">
      <p class="check-tip">
        <template v-if="currentRow?.item.itemStatus === 3">物品「{{ currentRow?.item.title }}」失主已在巡检前领取。请查看签字认领书与失物合影，确认凭证无误后补发 <b>1 积分</b>。</template>
        <template v-else>确认物品「{{ currentRow?.item.title }}」已实物在站且信息相符？核对通过后给发布人发 <b>1 积分</b>。</template>
      </p>
      <el-input v-model="checkNote" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="核对备注（可选）" />
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkSubmitting" @click="handleCheck">确认核对</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="takedownDialogVisible" title="管理员下架" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px" title="仅当发现重复投放、虚假招领、实物不在站时使用。下架必须填原因，原因与你的账号会写入留档。" />
      <p>下架物品：<b>{{ currentRow?.item.title }}</b>（#{{ currentRow?.item.id }}）</p>
      <el-input v-model="takedownReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请填写下架原因（必填）" />
      <template #footer>
        <el-button @click="takedownDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="takedownSubmitting" @click="handleTakedown">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { dropPointApi, moderationApi, archiveApi } from '@/api'
import type { DropPoint } from '@/types'

interface InventoryRow {
  item: any
  handInStatus: number
  handInAt?: string
  checkedAt?: string
  checkNote?: string
  creditIssued?: number
  founderName?: string
  claimerName?: string
  pickupTime?: string
}

const loading = ref(false)
const archiveLoading = ref(false)
const inventory = ref<InventoryRow[]>([])
const archiveList = ref<any[]>([])
const myPoints = ref<DropPoint[]>([])
const selectedPointId = ref<number | null>(null)
const activeTab = ref<'active' | 'archive'>('active')

const filter = ref({ keyword: '', category: '', biz: '' })

const checkDialogVisible = ref(false)
const checkSubmitting = ref(false)
const checkNote = ref('')
const currentRow = ref<InventoryRow | null>(null)
const takedownDialogVisible = ref(false)
const takedownSubmitting = ref(false)
const takedownReason = ref('')

const formatTime = (t: string) => t ? t.replace('T', ' ').slice(0, 16) : '-'

const categories = ['电子产品', '证件卡类', '钥匙', '书籍资料', '衣物', '饰品', '生活用品', '其他']

const WARRANTY_DAYS = 7
const withinWarranty = (row: InventoryRow): boolean => {
  if (!row.pickupTime) return true
  const d = new Date(row.pickupTime)
  if (isNaN(d.getTime())) return true
  const deadline = new Date(d.getTime() + WARRANTY_DAYS * 86400000)
  return deadline > new Date()
}
const isArchived = (row: InventoryRow) => {
  const s = row.item.itemStatus
  if (s === 0 || s === 5) return true
  if (s === 3) {
    // 已领取：已核对完结 或 过了7天保障期 → 入留档
    if (row.handInStatus === 2) return true
    if (!withinWarranty(row)) return true
  }
  return false
}

const stats = computed(() => {
  const s = { pending: 0, placed: 0, picked: 0, onSite: 0, checking: 0, checked: 0 }
  inventory.value.forEach(r => {
    if (isArchived(r)) return
    const st = r.item.itemStatus
    if (st === 6) s.pending++
    if (st === 1 || st === 2) s.placed++
    if (st === 3 && withinWarranty(r)) s.picked++
    if (st === 1 || st === 2) s.onSite++
    if (r.handInStatus === 1) s.checking++
    if (r.handInStatus === 2) s.checked++
  })
  return s
})

const activeList = computed(() => {
  return inventory.value.filter(r => {
    if (isArchived(r)) return false
    if (filter.value.keyword) {
      const k = filter.value.keyword.toLowerCase()
      const hay = `${r.item.title} ${r.item.description || ''} ${r.item.foundLocation || ''}`.toLowerCase()
      if (!hay.includes(k)) return false
    }
    if (filter.value.category && r.item.category !== filter.value.category) return false
    if (filter.value.biz) {
      const st = r.item.itemStatus
      if (filter.value.biz === 'pending' && st !== 6) return false
      if (filter.value.biz === 'placed' && !(st === 1 || st === 2)) return false
      if (filter.value.biz === 'picked' && !(st === 3 && withinWarranty(r))) return false
      if (filter.value.biz === 'checking' && r.handInStatus !== 1) return false
      if (filter.value.biz === 'checked' && r.handInStatus !== 2) return false
    }
    return true
  })
})


const bizTag = (row: InventoryRow) => {
  const st = row.item.itemStatus
  if (st === 6) return { type: 'info', text: '待投放' }
  if (st === 2) return { type: 'warning', text: '认领中' }
  if (st === 1) return { type: 'success', text: '已投放' }
  if (st === 3) return withinWarranty(row)
    ? { type: 'primary', text: '已领取(保障期内)' }
    : { type: 'info', text: '已领取(过保)' }
  return { type: 'info', text: '未知' }
}
const checkTag = (row: InventoryRow) => {
  if (row.handInStatus === 2) return { type: 'success', text: '已核对' }
  if (row.handInStatus === 1) return { type: 'warning', text: '待核对' }
  return { type: 'info', text: '未投放' }
}

const fetchMyPoints = async () => {
  const res = await dropPointApi.list()
  const all = res.data.filter(p => p.status === 1)
  myPoints.value = all
  if (myPoints.value.length && !selectedPointId.value) selectedPointId.value = myPoints.value[0].id
}

const fetchInventory = async () => {
  if (!selectedPointId.value) return
  loading.value = true
  try {
    const res = await dropPointApi.getInventory(selectedPointId.value)
    inventory.value = res.data as InventoryRow[]
  } finally { loading.value = false }
}

const fetchArchive = async () => {
  if (!selectedPointId.value) return
  archiveLoading.value = true
  try {
    const res = await archiveApi.listItems({ dropPointId: selectedPointId.value })
    archiveList.value = res.data
  } finally { archiveLoading.value = false }
}

const fetchAll = () => { fetchInventory(); fetchArchive() }

const openCheckDialog = (row: InventoryRow) => { currentRow.value = row; checkNote.value = ''; checkDialogVisible.value = true }
const handleCheck = async () => {
  if (!selectedPointId.value || !currentRow.value) return
  checkSubmitting.value = true
  try {
    await dropPointApi.checkItem(selectedPointId.value, { itemId: currentRow.value.item.id, checkNote: checkNote.value || undefined })
    ElMessage.success('巡检完成，已发放 1 积分')
    checkDialogVisible.value = false
    fetchAll()
  } finally { checkSubmitting.value = false }
}

const openTakedownDialog = (row: InventoryRow) => { currentRow.value = row; takedownReason.value = ''; takedownDialogVisible.value = true }
const handleTakedown = async () => {
  if (!takedownReason.value.trim()) { ElMessage.warning('请填写下架原因'); return }
  if (!currentRow.value) return
  takedownSubmitting.value = true
  try {
    await moderationApi.takedownItem(currentRow.value.item.id, takedownReason.value.trim())
    ElMessage.success('已下架，记录已入留档区')
    takedownDialogVisible.value = false
    fetchAll()
  } finally { takedownSubmitting.value = false }
}

const resetFilter = () => { filter.value = { keyword: '', category: '', biz: '' } }

watch(selectedPointId, () => fetchAll())
onMounted(async () => { await fetchMyPoints(); fetchAll() })
</script>

<style scoped>
.point-admin-container { max-width: 1400px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: bold; }
.stat-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #333; margin-top: 2px; }
.stat-sub { font-size: 11px; color: #999; margin-top: 2px; }
.stat-physical { background: #f0f9eb; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.item-link { color: #26745c; font-weight: 500; }
.sub-text { font-size: 12px; color: #999; margin-top: 2px; }
.check-tip { margin: 0 0 12px; line-height: 1.6; }
</style>
