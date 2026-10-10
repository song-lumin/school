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

    <div class="filter-bar">
      <div class="filter-search">
        <el-icon><Search /></el-icon>
        <input v-model="queryForm.keyword" placeholder="搜索物品名称" @keyup.enter="handleSearch" />
        <button v-if="queryForm.keyword" class="filter-clear" aria-label="清空关键词" @click="clearKeyword">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 136px" @change="handleSearch">
        <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="queryForm.dropPointId" placeholder="全部投放点" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="p in dropPoints" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-if="userStore.isLoggedIn" v-model="queryForm.itemStatus" placeholder="全部状态" clearable style="width: 140px" @change="handleSearch">
        <el-option label="公开待认领" :value="1" />
        <el-option label="认领中" :value="2" />
        <el-option label="已取件" :value="3" />
        <el-option label="已过期" :value="5" />
      </el-select>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading" class="list-body">
      <el-empty v-if="!loading && items.length === 0" description="没有符合条件的物品" :image-size="80" />
      <div v-else class="item-grid">
        <div v-for="item in items" :key="item.id" class="card-wrap">
          <button
            class="found-card"
            @click="$router.push(`/items/${item.id}`)"
          >
            <div class="found-visual" :class="`visual-${categoryTone(item.category)}`">
              <el-image
                v-if="firstImage(item) && !isPlaceholderImage(firstImage(item)!)"
                :src="firstImage(item)"
                fit="cover"
                lazy
              >
                <template #error><div class="visual-fallback"><el-icon><Box /></el-icon></div></template>
              </el-image>
              <div v-else class="visual-fallback"><el-icon><Box /></el-icon></div>
              <span class="visual-category">{{ item.category }}</span>
              <span class="visual-status" :data-status="item.itemStatus">{{ ITEM_STATUS_MAP[item.itemStatus]?.text }}</span>
            </div>
            <div class="found-card-copy">
              <span class="found-title">{{ item.title }}</span>
              <span class="found-location"><el-icon><Location /></el-icon>{{ item.foundLocation || '地点未填写' }}</span>
              <span class="found-meta">{{ formatTime(item.foundTime) }}<template v-if="item.dropPointName"> · {{ item.dropPointName }}</template></span>
            </div>
          </button>
        </div>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchItems"
          @current-change="fetchItems"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { foundItemApi, dropPointApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ITEM_STATUS_MAP, ITEM_CATEGORIES } from '@/types'
import type { FoundItem, DropPoint, ItemQueryRequest } from '@/types'
import { categoryTone } from '@/utils/placeholder'

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
  size: 12
})

const firstImage = (item: FoundItem) => item.images?.[0]
const isPlaceholderImage = (src: string) => src.includes('example.com')

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

const clearKeyword = () => {
  queryForm.keyword = ''
  handleSearch()
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

.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 14px 16px; margin-bottom: 22px; background: #fff; border: 1px solid #e2e9e4; }
.filter-search { display: flex; flex: 1; align-items: center; gap: 9px; min-width: 200px; max-width: 320px; height: 32px; padding: 0 4px 0 11px; background: #f3f7f4; border: 1px solid #dce5df; transition: border-color .2s; }
.filter-search:focus-within { border-color: #26745c; background: #fff; }
.filter-search .el-icon { color: #84958b; font-size: 15px; }
.filter-search input { flex: 1; min-width: 0; height: 100%; border: 0; outline: 0; background: transparent; color: #26332f; font: inherit; font-size: 13px; }
.filter-search input::placeholder { color: #9aa8a1; }
.filter-clear { display: grid; place-items: center; width: 24px; height: 24px; padding: 0; border: 0; background: transparent; color: #84958b; cursor: pointer; }
.filter-clear:hover { color: #26745c; }
.filter-bar .el-select { width: 140px; }

.list-body { min-height: 300px; }
.item-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 17px; align-items: start; }
.card-wrap { position: relative; }
.card-delete { position: absolute; top: 10px; right: 10px; z-index: 2; }
.found-card { display: block; overflow: hidden; padding: 0; border: 0; background: #fff; color: inherit; text-align: left; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.found-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgb(33 64 48 / 10%); }
.found-card:focus-visible { outline: 3px solid #26745c; outline-offset: 3px; }
.found-visual { position: relative; display: grid; place-items: center; height: 190px; overflow: hidden; }
.found-visual .el-image, .found-visual :deep(.el-image__inner) { width: 100%; height: 100%; transition: transform .35s ease; }
.found-card:hover .found-visual :deep(.el-image__inner) { transform: scale(1.04); }
.visual-fallback { display: grid; place-items: center; width: 100%; height: 100%; color: rgb(255 255 255 / 82%); }
.visual-fallback .el-icon { font-size: 50px; }
.visual-green { background: #81a78e; }
.visual-coral { background: #d88770; }
.visual-blue { background: #7899a5; }
.visual-yellow { background: #c3a668; }
.visual-category { position: absolute; right: 11px; bottom: 10px; padding: 5px 9px; background: rgb(255 255 255 / 90%); color: #43564b; font-size: 11px; }
.visual-status { position: absolute; top: 10px; left: 11px; padding: 4px 8px; font-size: 11px; font-weight: 550; color: #fff; }
.visual-status[data-status='1'] { background: rgb(38 116 92 / 92%); }
.visual-status[data-status='2'] { background: rgb(198 128 38 / 92%); }
.visual-status[data-status='3'] { background: rgb(90 115 138 / 92%); }
.visual-status[data-status='5'], .visual-status[data-status='0'], .visual-status[data-status='4'] { background: rgb(120 132 124 / 88%); }
.visual-status[data-status='6'] { background: rgb(198 128 38 / 92%); }
.found-card-copy { display: flex; flex-direction: column; gap: 7px; padding: 13px 14px 15px; }
.found-title { overflow: hidden; color: #2c3933; font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.found-location { display: flex; align-items: center; gap: 5px; overflow: hidden; color: #85928b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.found-meta { color: #9aa69f; font-size: 12px; }

.pagination-wrapper { display: flex; justify-content: center; margin-top: 26px; }

@media (max-width: 1100px) {
  .item-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 860px) {
  .item-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .filter-search { max-width: 100%; flex-basis: 100%; }
  .filter-bar .el-select { flex: 1; min-width: 110px; }
}
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading h1 { font-size: 23px; }
  .filter-bar { padding: 12px; }
  .filter-search { max-width: none; }
  .found-visual { height: 155px; }
  .pagination-wrapper { justify-content: flex-start; overflow-x: auto; }
}
@media (prefers-reduced-motion: reduce) {
  .found-card, .found-visual .el-image__inner { transition: none; }
}
</style>
