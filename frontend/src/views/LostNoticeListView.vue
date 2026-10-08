<template>
  <div class="notice-container">
    <div class="page-heading">
      <div>
        <h1>寻物启事</h1>
        <p>看看同学们正在寻找什么，也许你能提供线索</p>
      </div>
      <el-button v-if="userStore.isLoggedIn" type="primary" @click="publishDialogVisible = true">
        <el-icon><Plus /></el-icon> 发布启事
      </el-button>
    </div>
    <el-card class="image-search-panel">
      <div class="image-search-head">
        <div>
          <h2>用图片寻找相似物品</h2>
          <p>基于已登记物品照片的视觉相似度排序，不是物品语义识别</p>
        </div>
        <el-select v-model="imageCategory" clearable placeholder="全部分类" class="image-category">
          <el-option v-for="category in ITEM_CATEGORIES" :key="category" :label="category" :value="category" />
        </el-select>
      </div>
      <div class="image-search-controls">
        <input ref="imageInput" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/gif" @change="handleImageChange" />
        <el-button :icon="Picture" plain :loading="imageSearching" @click="imageInput?.click()">选择照片并搜索</el-button>
        <span v-if="selectedImageName" class="selected-image">{{ selectedImageName }}</span>
      </div>
      <div v-if="imageResults.length" class="image-results">
        <button v-for="result in imageResults" :key="result.item.id" class="image-result" @click="$router.push(`/items/${result.item.id}`)">
          <el-image v-if="result.item.images?.[0]" :src="result.item.images[0]" fit="cover" />
          <div v-else class="image-result-placeholder"><el-icon><Box /></el-icon></div>
          <span class="image-result-copy"><strong>{{ result.item.title }}</strong><small>{{ result.item.foundLocation }}</small></span>
          <el-tag size="small" type="success">相似 {{ result.similarity }}%</el-tag>
        </button>
      </div>
      <el-empty v-else-if="imageSearched" description="没有找到足够相似的图片" :image-size="52" />
    </el-card>
    <div class="filter-bar">
      <div class="filter-search">
        <el-icon><Search /></el-icon>
        <input v-model="queryForm.keyword" placeholder="搜索启事标题" @keyup.enter="handleSearch" />
        <button v-if="queryForm.keyword" class="filter-clear" aria-label="清空关键词" @click="clearKeyword">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading" class="list-body">
      <el-empty v-if="!loading && notices.length === 0" description="暂时没有寻物启事" :image-size="80" />
      <div v-else class="notice-grid">
        <div v-for="notice in notices" :key="notice.id" class="card-wrap">
          <button
            class="notice-card"
            @click="$router.push(`/notices/${notice.id}`)"
          >
            <div class="notice-visual" :class="`visual-${categoryTone(notice.category)}`">
              <el-image
                v-if="firstImage(notice) && !isPlaceholderImage(firstImage(notice)!)"
                :src="firstImage(notice)"
                fit="cover"
                lazy
              >
                <template #error><div class="visual-fallback"><el-icon><Search /></el-icon></div></template>
              </el-image>
              <div v-else class="visual-fallback"><el-icon><Search /></el-icon></div>
              <span class="visual-category">{{ notice.category }}</span>
              <span class="visual-status" :data-status="notice.status">{{ NOTICE_STATUS_MAP[notice.status]?.text }}</span>
            </div>
            <div class="notice-card-copy">
              <span class="notice-title">{{ notice.title }}</span>
              <span class="notice-location"><el-icon><Location /></el-icon>{{ notice.lostLocation || '地点未填写' }}</span>
              <span class="notice-meta">丢失于 {{ formatTime(notice.lostTime) }}<template v-if="notice.contactInfo"> · 联系 {{ notice.contactInfo }}</template></span>
            </div>
          </button>
          <el-button
            v-if="userStore.isAdmin"
            class="card-delete"
            type="danger"
            :icon="Delete"
            circle
            size="small"
            title="删除该启事"
            @click.stop="handleDelete(notice)"
          />
        </div>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchNotices"
          @current-change="fetchNotices"
        />
      </div>
    </div>

    <el-dialog v-model="publishDialogVisible" title="发布寻物启事" width="600px">
      <el-form ref="formRef" :model="publishForm" :rules="rules" label-width="100px">
        <el-form-item label="启事标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="例如：丢失黑色钱包" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="物品分类" prop="category">
          <el-select v-model="publishForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物品描述">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="3"
            placeholder="外观、特征等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="丢失地点">
          <el-input v-model="publishForm.lostLocation" placeholder="大概位置即可" maxlength="100" />
        </el-form-item>
        <el-form-item label="丢失时间">
          <el-date-picker
            v-model="publishForm.lostTime"
            type="datetime"
            placeholder="大概时间即可"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="联系方式" prop="contactInfo">
          <el-input v-model="publishForm.contactInfo" placeholder="微信 / QQ / 手机号" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { lostNoticeApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { NOTICE_STATUS_MAP, ITEM_CATEGORIES } from '@/types'
import type { LostNotice } from '@/types'
import { categoryTone } from '@/utils/placeholder'

const userStore = useUserStore()
const loading = ref(false)
const publishing = ref(false)
const notices = ref<LostNotice[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const imageInput = ref<HTMLInputElement>()
const imageCategory = ref('')
const imageSearching = ref(false)
const imageSearched = ref(false)
const selectedImageName = ref('')
const imageResults = ref<Array<{ item: import('@/types').FoundItem; similarity: number }>>([])

const queryForm = reactive({
  keyword: '',
  category: '',
  page: 1,
  size: 10
})

const publishDialogVisible = ref(false)
const publishForm = reactive({
  title: '',
  category: '',
  description: '',
  lostLocation: '',
  lostTime: '',
  contactInfo: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入启事标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择物品分类', trigger: 'change' }],
  contactInfo: [{ required: true, message: '请输入联系方式', trigger: 'blur' }]
}

const formatTime = (time?: string | null) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const firstImage = (notice: LostNotice) => notice.images?.[0]
const isPlaceholderImage = (src: string) => src.includes('example.com')

const clearKeyword = () => {
  queryForm.keyword = ''
  handleSearch()
}

const fetchNotices = async () => {
  loading.value = true
  try {
    const res = await lostNoticeApi.list({
      keyword: queryForm.keyword || undefined,
      category: queryForm.category || undefined,
      page: queryForm.page,
      size: queryForm.size
    })
    notices.value = res.data.records
    total.value = Number(res.data.total)
  } catch (error) {
    console.error('加载寻物启事失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  fetchNotices()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.page = 1
  fetchNotices()
}

const handleDelete = async (notice: LostNotice) => {
  try {
    await ElMessageBox.confirm(`确定删除启事「${notice.title}」吗？删除后不可恢复`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await lostNoticeApi.remove(notice.id)
    ElMessage.success('删除成功')
    fetchNotices()
  } catch (error) {
    console.error('删除启事失败:', error)
  }
}

const handleImageChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过10MB')
    input.value = ''
    return
  }
  selectedImageName.value = file.name
  imageSearching.value = true
  imageSearched.value = true
  try {
    const res = await lostNoticeApi.searchByImage(file, imageCategory.value || undefined)
    imageResults.value = res.data
  } catch (error) {
    console.error('图片搜索失败:', error)
  } finally {
    imageSearching.value = false
    input.value = ''
  }
}

const handlePublish = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  publishing.value = true
  try {
    await lostNoticeApi.publish({
      title: publishForm.title,
      category: publishForm.category,
      description: publishForm.description || undefined,
      lostLocation: publishForm.lostLocation || undefined,
      lostTime: publishForm.lostTime || undefined,
      contactInfo: publishForm.contactInfo
    })
    ElMessage.success('寻物启事发布成功')
    publishDialogVisible.value = false
    Object.assign(publishForm, {
      title: '',
      category: '',
      description: '',
      lostLocation: '',
      lostTime: '',
      contactInfo: ''
    })
    handleSearch()
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    publishing.value = false
  }
}

onMounted(fetchNotices)
</script>

<style scoped>
.notice-container {
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

.list-body { min-height: 300px; }
.notice-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 17px; align-items: start; }
.card-wrap { position: relative; }
.card-delete { position: absolute; top: 10px; right: 10px; z-index: 2; }
.notice-card { display: block; overflow: hidden; padding: 0; border: 0; background: #fff; color: inherit; text-align: left; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.notice-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgb(33 64 48 / 10%); }
.notice-card:focus-visible { outline: 3px solid #26745c; outline-offset: 3px; }
.notice-visual { position: relative; display: grid; place-items: center; height: 170px; overflow: hidden; }
.notice-visual .el-image, .notice-visual :deep(.el-image__inner) { width: 100%; height: 100%; transition: transform .35s ease; }
.notice-card:hover .notice-visual :deep(.el-image__inner) { transform: scale(1.04); }
.visual-fallback { display: grid; place-items: center; width: 100%; height: 100%; color: rgb(255 255 255 / 82%); }
.visual-fallback .el-icon { font-size: 46px; }
.visual-green { background: #81a78e; }
.visual-coral { background: #d88770; }
.visual-blue { background: #7899a5; }
.visual-yellow { background: #c3a668; }
.visual-category { position: absolute; right: 11px; bottom: 10px; padding: 5px 9px; background: rgb(255 255 255 / 90%); color: #43564b; font-size: 11px; }
.visual-status { position: absolute; top: 10px; left: 11px; padding: 4px 8px; font-size: 11px; font-weight: 550; color: #fff; }
.visual-status[data-status='0'] { background: rgb(38 116 92 / 92%); }
.visual-status[data-status='1'] { background: rgb(90 115 138 / 92%); }
.visual-status[data-status='2'] { background: rgb(120 132 124 / 88%); }
.notice-card-copy { display: flex; flex-direction: column; gap: 7px; padding: 13px 14px 15px; }
.notice-title { overflow: hidden; color: #2c3933; font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.notice-location { display: flex; align-items: center; gap: 5px; overflow: hidden; color: #85928b; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.notice-meta { overflow: hidden; color: #9aa69f; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }

.pagination-wrapper { display: flex; justify-content: center; margin-top: 26px; }

.image-search-panel { margin-bottom: 18px; }
.image-search-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.image-search-head h2 { color: #304039; font-size: 16px; font-weight: 600; }
.image-search-head p { margin-top: 5px; color: #89958f; font-size: 12px; }
.image-category { width: 170px; }
.image-search-controls { display: flex; align-items: center; gap: 12px; margin-top: 15px; }
.selected-image { overflow: hidden; color: #78877f; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
.image-results { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 20px; margin-top: 15px; }
.image-result { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 8px; border: 1px solid #e5ebe7; background: #fff; text-align: left; cursor: pointer; }
.image-result > :deep(.el-image), .image-result-placeholder { flex: 0 0 48px; width: 48px; height: 48px; }
.image-result-placeholder { display: grid; place-items: center; background: #e7f0e9; color: #4c8769; }
.image-result-copy { display: flex; flex: 1; flex-direction: column; gap: 4px; min-width: 0; }
.image-result-copy strong, .image-result-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.image-result-copy strong { color: #34423b; font-size: 13px; }
.image-result-copy small { color: #89958f; font-size: 11px; }

@media (max-width: 1100px) {
  .notice-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 860px) {
  .notice-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .filter-search { max-width: 100%; flex-basis: 100%; }
  .filter-bar .el-select { flex: 1; min-width: 110px; }
}
@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading h1 { font-size: 23px; }
  .page-heading .el-button { flex: 0 0 auto; }
  .filter-bar { padding: 12px; }
  .filter-search { max-width: none; }
  .notice-visual { height: 145px; }
  .pagination-wrapper { justify-content: flex-start; overflow-x: auto; }
  .image-search-head { align-items: flex-start; flex-direction: column; }
  .image-category { width: 100%; }
  .image-results { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .notice-card, .notice-visual .el-image__inner { transition: none; }
}
</style>
