<template>
  <div class="point-admin-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>投放点工作台</span>
          <el-select
            v-model="selectedPointId"
            placeholder="选择站点"
            style="width: 240px"
            @change="fetchItems"
          >
            <el-option v-for="p in myPoints" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </template>

      <el-row :gutter="16" class="stat-row">
        <el-col :span="8">
          <el-card shadow="never">
            <el-statistic title="待交物" :value="pendingHandInCount" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <el-statistic title="待核对发分" :value="pendingCheckCount" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <el-statistic title="在站物品总数" :value="items.length" />
          </el-card>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="物品巡检" name="check">
          <el-table :data="items" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="图片" width="80">
              <template #default="{ row }">
                <el-image
                  v-if="row.images && row.images.length"
                  :src="row.images[0]"
                  :preview-src-list="row.images"
                  fit="cover"
                  style="width: 48px; height: 48px; border-radius: 4px"
                  preview-teleported
                />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="物品" min-width="160" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="foundLocation" label="拾获位置" width="140" show-overflow-tooltip />
            <el-table-column prop="publishedAt" label="发布时间" width="160">
              <template #default="{ row }">{{ formatTime(row.publishedAt) }}</template>
            </el-table-column>
            <el-table-column prop="itemStatus" label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="ITEM_STATUS_MAP[row.itemStatus]?.type || 'info'">
                  {{ ITEM_STATUS_MAP[row.itemStatus]?.text || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :disabled="row.itemStatus !== 1"
                  @click="openCheckDialog(row)"
                >
                  核对发分
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待巡检物品" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`待取件确认${pendingPickups.length ? ` (${pendingPickups.length})` : ''}`" name="pickup">
          <el-table :data="pendingPickups" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="itemTitle" label="物品" min-width="160" show-overflow-tooltip />
            <el-table-column prop="claimerName" label="领取人" width="110" />
            <el-table-column prop="answer" label="认领答案" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="openPickupDialog(row)">确认取件</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待取件申请" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="checkDialogVisible" title="巡检核对" width="440px">
      <p class="check-tip">
        确认物品「{{ currentItem?.title }}」已实物在站且信息相符？
        核对通过后将给拾获者发放 <b>1 积分</b>。
      </p>
      <el-input
        v-model="checkNote"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        placeholder="核对备注（可选）"
      />
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkSubmitting" @click="handleCheck">确认核对</el-button>
      </template>
    </el-dialog>

    <PickupDialog
      ref="pickupDialogRef"
      v-model:visible="pickupDialogVisible"
      :claim-id="pickupClaim?.id ?? null"
      @success="fetchPendingPickups"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { dropPointApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ITEM_STATUS_MAP } from '@/types'
import type { FoundItem, DropPoint, ClaimApply } from '@/types'
import PickupDialog from '@/components/PickupDialog.vue'

const userStore = useUserStore()
const loading = ref(false)
const items = ref<FoundItem[]>([])
const myPoints = ref<DropPoint[]>([])
const selectedPointId = ref<number | null>(null)
const activeTab = ref<'check' | 'pickup'>('check')

const checkDialogVisible = ref(false)
const checkSubmitting = ref(false)
const checkNote = ref('')
const currentItem = ref<FoundItem | null>(null)

const pendingPickups = ref<ClaimApply[]>([])
const pickupDialogVisible = ref(false)
const pickupClaim = ref<ClaimApply | null>(null)
const pickupDialogRef = ref<InstanceType<typeof PickupDialog> | null>(null)

const pendingHandInCount = computed(() => items.value.filter(i => i.itemStatus === 6).length)
const pendingCheckCount = computed(() => items.value.filter(i => i.itemStatus === 1).length)

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const fetchMyPoints = async () => {
  const res = await dropPointApi.list()
  const all = res.data.filter(p => p.status === 1)
  if (userStore.isAdmin) {
    myPoints.value = all
  } else {
    myPoints.value = all.filter(p => p.adminId === userStore.userInfo?.id)
  }
  if (myPoints.value.length > 0) {
    selectedPointId.value = myPoints.value[0].id
  }
}

const fetchItems = async () => {
  if (!selectedPointId.value) return
  loading.value = true
  try {
    const res = await dropPointApi.getPendingItems(selectedPointId.value)
    items.value = res.data
  } catch (error) {
    console.error('加载待巡检物品失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchPendingPickups = async () => {
  if (!selectedPointId.value) return
  try {
    const res = await dropPointApi.getPendingPickups(selectedPointId.value)
    pendingPickups.value = res.data
  } catch (error) {
    console.error('加载待取件申请失败:', error)
  }
}

const openCheckDialog = (row: FoundItem) => {
  currentItem.value = row
  checkNote.value = ''
  checkDialogVisible.value = true
}

const handleCheck = async () => {
  if (!selectedPointId.value || !currentItem.value) return
  checkSubmitting.value = true
  try {
    await dropPointApi.checkItem(selectedPointId.value, {
      itemId: currentItem.value.id,
      checkNote: checkNote.value || undefined
    })
    ElMessage.success('巡检完成，已给拾获者发放 1 积分')
    checkDialogVisible.value = false
    await fetchItems()
  } catch (error) {
    console.error('核对失败:', error)
  } finally {
    checkSubmitting.value = false
  }
}

const openPickupDialog = (row: ClaimApply) => {
  pickupClaim.value = row
  pickupDialogVisible.value = true
  pickupDialogRef.value?.init()
}

watch(selectedPointId, () => {
  fetchItems()
  fetchPendingPickups()
})

onMounted(async () => {
  await fetchMyPoints()
  fetchItems()
  fetchPendingPickups()
})
</script>

<style scoped>
.point-admin-container {
  max-width: 1300px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.stat-row {
  margin-bottom: 16px;
}

.check-tip {
  margin: 0 0 12px;
  line-height: 1.6;
}
</style>
