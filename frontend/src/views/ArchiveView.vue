<template>
  <div class="archive-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h3 style="margin:0">全局留档区</h3>
            <p style="margin:4px 0 0;color:#909399;font-size:12px">已完结、超期、下架的招领物品统一归档，保留下架原因与执行者账号，供审计、申诉与监控追溯。</p>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-input v-model="filter.keyword" placeholder="搜索物品名/描述/拾获位置" style="width:240px" clearable @keyup.enter="fetchData" />
        <el-select v-model="filter.category" placeholder="分类" clearable style="width:130px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filter.dropPointId" placeholder="投放点" clearable style="width:180px">
          <el-option v-for="p in points" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-select v-model="filter.reason" placeholder="留档原因" clearable style="width:150px">
          <el-option label="已领取完结" value="picked" />
          <el-option label="超期处置" value="expired" />
          <el-option label="管理员下架" value="takedown" />
          <el-option label="作废" value="voided" />
        </el-select>
        <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="至" start-placeholder="发布起" end-placeholder="发布止" value-format="YYYY-MM-DD" style="width:260px" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-row :gutter="12" class="stat-row">
        <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#67c23a">{{ stats.picked }}</div><div class="stat-label">已领取完结</div></el-card></el-col>
        <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#e6a23c">{{ stats.expired }}</div><div class="stat-label">超期处置</div></el-card></el-col>
        <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#f56c6c">{{ stats.takedown }}</div><div class="stat-label">下架/作废</div></el-card></el-col>
        <el-col :span="6"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#409eff">{{ list.length }}</div><div class="stat-label">筛选结果总数</div></el-card></el-col>
      </el-row>

      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image v-if="row.images && row.images.length" :src="row.images[0]" :preview-src-list="row.images" fit="cover" style="width:48px;height:48px;border-radius:4px" preview-teleported />
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="物品" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/items/${row.id}`" class="item-link">{{ row.title }}</router-link>
            <div class="sub-text">{{ row.foundLocation }} · {{ formatTime(row.publishedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }"><el-tag size="small">{{ row.category || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="dropPointName" label="所属投放点" width="160">
          <template #default="{ row }">{{ row.dropPointName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="founderName" label="发布人" width="110" />
        <el-table-column label="留档原因" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.itemStatus === 0 ? 'danger' : row.itemStatus === 5 ? 'warning' : 'success'">
              {{ row.itemStatus === 0 ? '下架/作废' : row.itemStatus === 5 ? '超期处置' : '已领取' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下架原因/执行者" min-width="240">
          <template #default="{ row }">
            <div v-if="row.takedownReason" class="sub-text">
              {{ row.takedownReason }}
              <span v-if="row.takedownByName" style="color:#c0392b">（{{ row.takedownByName }}）</span>
              <div v-if="row.takedownAt" style="color:#bbb;font-size:11px">{{ formatTime(row.takedownAt) }}</div>
            </div>
            <span v-else class="sub-text">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { archiveApi, dropPointApi } from '@/api'

interface ArchiveItem {
  id: number
  title: string
  category?: string
  images?: string[]
  foundLocation?: string
  publishedAt?: string
  itemStatus: number
  dropPointId?: number
  dropPointName?: string
  founderName?: string
  takedownReason?: string
  takedownAt?: string
  takedownByName?: string
}

const loading = ref(false)
const list = ref<ArchiveItem[]>([])
const points = ref<any[]>([])
const filter = ref({
  keyword: '',
  category: '',
  dropPointId: null as number | null,
  reason: '',
  dateRange: null as string[] | null
})

const formatTime = (t?: string) => t ? t.replace('T', ' ').slice(0, 16) : '-'

const categories = computed(() => {
  const s = new Set<string>()
  list.value.forEach(r => r.category && s.add(r.category))
  return Array.from(s)
})

const stats = computed(() => {
  const s = { picked: 0, expired: 0, takedown: 0 }
  list.value.forEach(r => {
    if (r.itemStatus === 3) s.picked++
    else if (r.itemStatus === 5) s.expired++
    else if (r.itemStatus === 0) s.takedown++
  })
  return s
})

const filteredList = computed(() => {
  return list.value.filter(r => {
    if (filter.value.reason) {
      const st = r.itemStatus
      if (filter.value.reason === 'picked' && st !== 3) return false
      if (filter.value.reason === 'expired' && st !== 5) return false
      if (filter.value.reason === 'takedown' && !(st === 0 && r.takedownReason)) return false
      if (filter.value.reason === 'voided' && st !== 0) return false
    }
    return true
  })
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { size: 500 }
    if (filter.value.keyword) params.keyword = filter.value.keyword
    if (filter.value.category) params.category = filter.value.category
    if (filter.value.dropPointId) params.dropPointId = filter.value.dropPointId
    if (filter.value.dateRange && filter.value.dateRange.length === 2) {
      params.from = filter.value.dateRange[0]
      params.to = filter.value.dateRange[1]
    }
    const res = await archiveApi.listItems(params)
    list.value = res.data || []
  } catch (e) {
    console.error('加载留档失败', e)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filter.value = { keyword: '', category: '', dropPointId: null, reason: '', dateRange: null }
  fetchData()
}

onMounted(async () => {
  try {
    const res = await dropPointApi.list()
    points.value = res.data
  } catch {}
  fetchData()
})
</script>

<style scoped>
.archive-container { max-width: 1400px; margin: 0 auto; }
.card-header h3 { margin: 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.stat-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 26px; font-weight: 700; }
.stat-label { font-size: 13px; color: #666; margin-top: 2px; }
.item-link { color: #26745c; font-weight: 500; }
.sub-text { font-size: 12px; color: #999; margin-top: 2px; }
</style>
