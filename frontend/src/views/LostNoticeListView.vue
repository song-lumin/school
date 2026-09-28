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
    <el-card class="list-panel">
      <el-form :inline="true" :model="queryForm" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="启事标题"
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="notices" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="启事" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lostLocation" label="丢失地点" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.lostLocation || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lostTime" label="丢失时间" width="170">
          <template #default="{ row }">{{ formatTime(row.lostTime) }}</template>
        </el-table-column>
        <el-table-column prop="contactInfo" label="联系方式" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="NOTICE_STATUS_MAP[row.status]?.type || 'info'">
              {{ NOTICE_STATUS_MAP[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/notices/${row.id}`)">详情</el-button>
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
          @size-change="fetchNotices"
          @current-change="fetchNotices"
        />
      </div>
    </el-card>

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
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { lostNoticeApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { NOTICE_STATUS_MAP, ITEM_CATEGORIES } from '@/types'
import type { LostNotice } from '@/types'

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
.list-panel :deep(.el-card__body) { padding: 20px; }
.list-panel :deep(.el-form) { display: flex; flex-wrap: wrap; align-items: center; gap: 0 8px; }
.list-panel :deep(.el-form-item) { margin-bottom: 12px; }
.list-panel :deep(.el-table) { overflow-x: auto; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

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

@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading h1 { font-size: 23px; }
  .page-heading .el-button { flex: 0 0 auto; }
  .list-panel :deep(.el-card__body) { padding: 14px; }
  .pagination-wrapper { justify-content: flex-start; overflow-x: auto; }
  .image-search-head { align-items: flex-start; flex-direction: column; }
  .image-category { width: 100%; }
  .image-results { grid-template-columns: 1fr; }
}
</style>
