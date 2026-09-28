<template>
  <div class="item-list-container">
    <div class="page-heading">
      <div>
        <h1>失物招领</h1>
        <p>浏览校园里已登记、等待认领的物品</p>
      </div>
      <el-button v-if="userStore.isLoggedIn" type="primary" @click="$router.push('/items/publish')">
        <el-icon><Plus /></el-icon> 发布招领
      </el-button>
    </div>
    <el-card class="list-panel">
      <el-form :inline="true" :model="queryForm" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="物品标题"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="投放点">
          <el-select v-model="queryForm.dropPointId" placeholder="全部站点" clearable style="width: 160px">
            <el-option v-for="p in dropPoints" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userStore.isLoggedIn" label="状态">
          <el-select v-model="queryForm.itemStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="公开待认领" :value="1" />
            <el-option label="认领中" :value="2" />
            <el-option label="已取件" :value="3" />
            <el-option label="已过期" :value="5" />
            <el-option label="待交物" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="items" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="物品" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.title }}
            <el-tag v-if="row.newUser" size="small" type="warning">新手发布</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="foundLocation" label="拾取地点" width="140" show-overflow-tooltip />
        <el-table-column prop="foundTime" label="拾取时间" width="170">
          <template #default="{ row }">{{ formatTime(row.foundTime) }}</template>
        </el-table-column>
        <el-table-column prop="dropPointName" label="投放点" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.dropPointName || '未投放' }}</template>
        </el-table-column>
        <el-table-column prop="itemStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="ITEM_STATUS_MAP[row.itemStatus]?.type || 'info'">
              {{ ITEM_STATUS_MAP[row.itemStatus]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/items/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchItems"
          @current-change="fetchItems"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { foundItemApi, dropPointApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ITEM_STATUS_MAP, ITEM_CATEGORIES } from '@/types'
import type { FoundItem, DropPoint, ItemQueryRequest } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const items = ref<FoundItem[]>([])
const dropPoints = ref<DropPoint[]>([])
const total = ref(0)

const queryForm = reactive<ItemQueryRequest>({
  keyword: '',
  category: '',
  dropPointId: undefined,
  itemStatus: undefined,
  page: 1,
  size: 10
})

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await foundItemApi.list({
      keyword: queryForm.keyword || undefined,
      category: queryForm.category || undefined,
      dropPointId: queryForm.dropPointId,
      itemStatus: queryForm.itemStatus,
      page: queryForm.page,
      size: queryForm.size
    })
    items.value = res.data.records
    total.value = Number(res.data.total)
  } catch (error) {
    console.error('加载物品列表失败:', error)
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

const handleSearch = () => {
  queryForm.page = 1
  fetchItems()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.dropPointId = undefined
  queryForm.itemStatus = undefined
  queryForm.page = 1
  fetchItems()
}

onMounted(() => {
  fetchItems()
  fetchDropPoints()
})
</script>

<style scoped>
.item-list-container {
  max-width: 1240px;
  margin: 0 auto;
}

.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 4px 0 20px; }
.page-heading h1 { color: #26332f; font-size: 27px; font-weight: 650; }
.page-heading p { margin-top: 6px; color: #7d8a83; font-size: 13px; }
.list-panel :deep(.el-card__body) { padding: 20px; }
.list-panel :deep(.el-form) { display: flex; flex-wrap: wrap; align-items: center; gap: 0 8px; }
.list-panel :deep(.el-form-item) { margin-bottom: 12px; }
.list-panel :deep(.el-table) { overflow-x: auto; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading h1 { font-size: 23px; }
  .page-heading .el-button { flex: 0 0 auto; }
  .list-panel :deep(.el-card__body) { padding: 14px; }
  .pagination-wrapper { justify-content: flex-start; overflow-x: auto; }
}
</style>
