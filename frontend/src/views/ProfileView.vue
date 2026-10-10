<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>个人中心</h3>
          <el-radio-group v-model="activeTab">
            <el-radio-button value="info">我的资料</el-radio-button>
            <el-radio-button value="credits">积分台账</el-radio-button>
            <el-radio-button v-if="isNormalUser" value="cert">诚信证书</el-radio-button>
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
          <el-descriptions-item v-if="isNormalUser" label="诚信积分">
            <el-tag type="warning">{{ userStore.userInfo.creditScore }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="isNormalUser" label="光荣榜展示">
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
          <el-button v-if="isNormalUser" type="success" plain @click="$router.push('/certificates')">诚信证书</el-button>
        </div>
      </template>

      <template v-else-if="activeTab === 'credits'">
        <el-collapse class="credit-rules" v-model="rulesOpen">
          <el-collapse-item title="积分规则说明（点此展开/收起）" name="rules">
            <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
              <el-descriptions-item label="发布招领奖励" span="2">+1 分。拾得者发布招领并把实物放入投放点后，由点位管理员巡检核对实物在站时发放；若失主在巡检前已先领取，管理员核对领取凭证后补发。</el-descriptions-item>
              <el-descriptions-item label="成功归还奖励（拾取者）" span="2">+3 分。失主现场核对无误、签字合影确认领取后，自动发放给招领发布人。</el-descriptions-item>
              <el-descriptions-item label="成功归还奖励（失主）" span="2">+1 分。失主现场确认领取后自动发放，鼓励及时认领。</el-descriptions-item>
              <el-descriptions-item label="转发推荐奖励" span="2">+1 分。把公开招领转发到某条寻物启事，若该启事发布者最终通过此转发链路认领成功，转发者自动获得 1 分（回滚规则同上）。</el-descriptions-item>
              <el-descriptions-item label="扣分/回滚" span="2">虚假招领、举报成立、申诉判定发错实物：已发积分将回滚；答错防伪问题累计 3 次锁定 24 小时（不直接扣分，但会降低认领置信度）。</el-descriptions-item>
            </el-descriptions>
            <p style="color:#89958f;font-size:12px;margin:0">
              台账"类型"列对应：CHECK_ISSUE=巡检核对发的发布奖励；PICKUP_ISSUE=领取完成时的拾取者/失主奖励；FORWARD_REWARD=转发推荐奖励；带负数的为回滚/扣减。
            </p>
          </el-collapse-item>
        </el-collapse>
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

      <template v-if="activeTab === 'cert'">
        <el-alert v-if="certNotice" type="info" :closable="false" style="margin-bottom:14px" :title="certNotice" />
        <div v-else-if="cert" style="text-align:center;padding:20px">
          <h2 style="color:#26745c">诚信积分证明</h2>
          <p style="font-size:18px;font-weight:600;margin:10px 0">{{ cert.realName }}</p>
          <p style="color:#666">学号：{{ cert.studentId }}</p>
          <div style="font-size:48px;font-weight:700;color:#26745c;margin:16px 0">{{ cert.totalScore }}</div>
          <p style="color:#999">诚信积分</p>
          <el-button type="primary" style="margin-top:16px" @click="$router.push('/certificates')">查看完整证书与光荣榜</el-button>
        </div>
        <el-skeleton v-else :rows="5" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi, creditApi } from '@/api'
import type { CreditLog } from '@/types'

const userStore = useUserStore()

const isNormalUser = computed(() => userStore.userInfo?.role === 'USER')

const activeTab = ref<'info' | 'credits' | 'cert'>('info')
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
const cert = ref<any>(null)
const certNotice = ref('')
const certLoading = ref(false)
const leaderboard = ref<any[]>([])
const rulesOpen = ref(['rules'])

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

const fetchLeaderboard = async () => {
  try {
    const { creditApi } = await import('@/api')
    const res = await creditApi.getLeaderboard(100)
    leaderboard.value = res.data.entries || []
  } catch (e) { console.error(e) }
}
const fetchCert = async () => {
  certLoading.value = true
  certNotice.value = ''
  try {
    const { creditApi } = await import('@/api')
    const res = await creditApi.getCertificate()
    cert.value = res.data
  } catch (e: any) {
    if (e?.response?.status === 403) {
      certNotice.value = '管理员/点位管理员不参与诚信积分体系，无需诚信证明。'
    } else {
      console.error('加载证书失败', e)
    }
  } finally {
    certLoading.value = false
  }
}
watch(activeTab, (tab) => {
  if (tab === 'credits' && creditLogs.value.length === 0) {
    fetchCreditLogs()
  }
  if (tab === 'cert') {
    if (!cert.value && !certNotice.value) fetchCert()
    if (leaderboard.value.length === 0) fetchLeaderboard()
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
