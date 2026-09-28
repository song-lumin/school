<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>个人信息</h3>
          <el-radio-group v-model="activeTab">
            <el-radio-button value="info">我的资料</el-radio-button>
            <el-radio-button value="credits">积分台账</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <template v-if="activeTab === 'info'">
        <el-descriptions :column="2" border v-if="userStore.userInfo">
          <el-descriptions-item label="用户名">
            {{ userStore.userInfo.username }}
          </el-descriptions-item>
          <el-descriptions-item label="真实姓名">
            {{ userStore.userInfo.realName }}
          </el-descriptions-item>
          <el-descriptions-item label="学号/工号">
            {{ userStore.userInfo.studentId }}
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ userStore.userInfo.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ userStore.userInfo.email || '未设置' }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag v-if="userStore.userInfo.role === 'USER'">普通用户</el-tag>
            <el-tag type="success" v-else-if="userStore.userInfo.role === 'POINT_ADMIN'">站点管理员</el-tag>
            <el-tag type="danger" v-else-if="userStore.userInfo.role === 'SYS_ADMIN'">系统管理员</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="诚信积分">
            <el-tag type="warning">{{ userStore.userInfo.creditScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="光荣榜展示">
            <el-switch
              v-model="allowLeaderboard"
              :active-value="1"
              :inactive-value="0"
              :loading="leaderboardSaving"
              @change="handleLeaderboardChange"
            />
            <span class="field-tip">{{ allowLeaderboard === 1 ? '展示在光荣榜' : '不在光荣榜展示' }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px">
          <el-button type="primary" @click="openEditDialog">修改信息</el-button>
          <el-button @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button type="success" plain @click="$router.push('/certificates')">诚信证书</el-button>
        </div>
      </template>

      <template v-else>
        <el-alert
          title="巡检核对 +1 分；成功归还被领取，发布人 +3 分、失主 +1 分"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />
        <el-table :data="creditLogs" v-loading="creditLoading" style="width: 100%">
          <el-table-column prop="createdAt" label="时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="operationType" label="类型" width="140" />
          <el-table-column prop="changeAmount" label="变动" width="100">
            <template #default="{ row }">
              <span :class="row.changeAmount >= 0 ? 'amount-plus' : 'amount-minus'">
                {{ row.changeAmount >= 0 ? '+' : '' }}{{ row.changeAmount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="creditPage"
            v-model:page-size="creditSize"
            :total="creditTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchCreditLogs"
            @current-change="fetchCreditLogs"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="editDialogVisible" title="修改个人信息" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="真实姓名">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile" :loading="loading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi, creditApi } from '@/api'
import type { CreditLog } from '@/types'

const userStore = useUserStore()

const activeTab = ref<'info' | 'credits'>('info')
const loading = ref(false)
const editDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const allowLeaderboard = ref(userStore.userInfo?.allowLeaderboard ?? 1)
const leaderboardSaving = ref(false)

const creditLogs = ref<CreditLog[]>([])
const creditLoading = ref(false)
const creditPage = ref(1)
const creditSize = ref(10)
const creditTotal = ref(0)

const editForm = reactive({
  realName: userStore.userInfo?.realName || '',
  phone: userStore.userInfo?.phone || '',
  email: userStore.userInfo?.email || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').slice(0, 16)
}

const openEditDialog = () => {
  if (userStore.userInfo) {
    editForm.realName = userStore.userInfo.realName
    editForm.phone = userStore.userInfo.phone
    editForm.email = userStore.userInfo.email
  }
  editDialogVisible.value = true
}

const handleUpdateProfile = async () => {
  loading.value = true
  try {
    const res = await authApi.updateProfile(editForm)
    userStore.setUserInfo(res.data)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
  } catch (error) {
    console.error('修改失败:', error)
  } finally {
    loading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }

  loading.value = true
  try {
    await authApi.updatePassword(passwordForm)
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    setTimeout(() => {
      userStore.clearAuth()
      window.location.href = '/login'
    }, 1500)
  } catch (error) {
    console.error('修改失败:', error)
  } finally {
    loading.value = false
  }
}

const handleLeaderboardChange = async (val: number | string | boolean | undefined) => {
  leaderboardSaving.value = true
  try {
    await creditApi.setLeaderboardSetting(Number(val))
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, allowLeaderboard: Number(val) })
    }
    ElMessage.success(Number(val) === 1 ? '已开启光荣榜展示' : '已关闭光荣榜展示')
  } catch (error) {
    allowLeaderboard.value = allowLeaderboard.value === 1 ? 0 : 1
    console.error('设置失败:', error)
  } finally {
    leaderboardSaving.value = false
  }
}

const fetchCreditLogs = async () => {
  creditLoading.value = true
  try {
    const res = await creditApi.getMyLogs({ page: creditPage.value, size: creditSize.value })
    creditLogs.value = res.data.records
    creditTotal.value = Number(res.data.total)
  } catch (error) {
    console.error('加载积分台账失败:', error)
  } finally {
    creditLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'credits' && creditLogs.value.length === 0) {
    fetchCreditLogs()
  }
})

onMounted(() => {
  allowLeaderboard.value = userStore.userInfo?.allowLeaderboard ?? 1
})
</script>

<style scoped>
.profile-container {
  max-width: 1100px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.field-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 10px;
}

.amount-plus {
  color: #67c23a;
  font-weight: bold;
}

.amount-minus {
  color: #f56c6c;
  font-weight: bold;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
