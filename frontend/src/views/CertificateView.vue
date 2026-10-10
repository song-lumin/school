<template>
  <div class="certificate-container">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>我的诚信证明</h3>
              <el-tag v-if="myRank" type="warning">光荣榜第 {{ myRank }} 名</el-tag>
            </div>
          </template>
          <el-alert v-if="adminNotice" type="info" :closable="false" style="margin-bottom:14px" :title="adminNotice" />
          <div class="certificate-preview" id="certificate-area" v-if="!adminNotice">
            <div class="cert-inner">
              <h2>诚信积分证明</h2>
              <p class="cert-name" v-if="cert">{{ cert.realName }}</p>
              <p class="cert-id" v-if="cert">学号：{{ cert.studentId }}</p>
              <div class="cert-score">
                <span class="score-num">{{ cert?.totalScore ?? 0 }}</span>
                <span class="score-label">诚信积分</span>
              </div>
              <div class="cert-stats" v-if="cert && Object.keys(cert.statsByType).length">
                <p class="stats-title">获得方式统计</p>
                <p class="stats-line">
                  <span v-for="(count, type) in cert.statsByType" :key="type" class="stats-item">
                    {{ type }}：{{ count }} 次
                  </span>
                </p>
              </div>
              <div class="cert-records" v-if="cert && cert.records.length">
                <p class="stats-title">光荣行为记录（共 {{ cert.records.length }} 条）</p>
                <p class="record-line" v-for="(r, i) in cert.records.slice(0, 10)" :key="i">
                  {{ formatTime(r.createdAt) }}　{{ r.reason }}　+{{ r.changeAmount }} 分
                </p>
                <p class="record-line" v-if="cert.records.length > 10">
                  ……其余 {{ cert.records.length - 10 }} 条略
                </p>
              </div>
              <p class="cert-footer">校园失物招领及诚信积分系统 · {{ formatTime(cert?.issuedAt) }}</p>
            </div>
          </div>
          <div class="cert-actions" v-if="!adminNotice">
            <el-button type="primary" @click="handlePrint">
              <el-icon><Printer /></el-icon>
              打印 / 导出 PDF
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>诚信光荣榜 TOP {{ leaderboard.length || 100 }}</h3>
              <el-tag type="warning">按诚信积分排序</el-tag>
            </div>
          </template>
          <el-empty description="暂无数据" v-if="!loading && leaderboard.length === 0" />
          <el-table :data="rankedBoard" v-loading="loading" style="width: 100%" max-height="560">
            <el-table-column prop="rank" label="排名" width="80">
              <template #default="{ row }">
                <span :class="['rank-badge', `rank-${row.rank}`]">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="realName" label="姓名" min-width="110" />
            <el-table-column prop="creditScore" label="积分" width="100">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.creditScore }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { creditApi } from '@/api'
import type { LeaderboardEntry, CertificateVO } from '@/types'

const loading = ref(false)
const leaderboard = ref<LeaderboardEntry[]>([])
const myRank = ref<number | null>(null)
const cert = ref<CertificateVO | null>(null)

const formatTime = (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '')

const rankedBoard = computed(() =>
  leaderboard.value.map((e, idx) => ({ ...e, rank: idx + 1 }))
)

const fetchLeaderboard = async () => {
  loading.value = true
  try {
    const res = await creditApi.getLeaderboard(100)
    leaderboard.value = res.data.entries || []
    myRank.value = res.data.myRank ?? null
  } catch (error) {
    console.error('加载光荣榜失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePrint = () => {
  const area = document.getElementById('certificate-area')
  if (!area) return
  const win = window.open('', '_blank', 'width=800,height=600')
  if (!win) return
  win.document.write(`
    <html>
      <head>
        <title>诚信积分证明</title>
        <style>
          body { font-family: "Microsoft YaHei", sans-serif; display: flex; justify-content: center; padding: 40px; }
          .cert { text-align: center; border: 3px double #b88230; border-radius: 8px; padding: 60px 80px; }
          h2 { color: #b88230; letter-spacing: 4px; margin-bottom: 30px; }
          .name { font-size: 24px; font-weight: bold; margin: 16px 0; }
          .sid { color: #666; margin-bottom: 24px; }
          .score { font-size: 56px; font-weight: bold; color: #e6a23c; }
          .score-label { display: block; color: #999; font-size: 14px; }
          .footer { color: #b88230; margin-top: 32px; font-size: 14px; }
        </style>
      </head>
      <body>${area.innerHTML}</body>
    </html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => {
    win.print()
    win.close()
  }, 300)
}

const adminNotice = ref('')
const fetchCertificate = async () => {
  try {
    const res = await creditApi.getCertificate()
    cert.value = res.data
  } catch (error: any) {
    if (error?.response?.status === 403) {
      adminNotice.value = '管理员/点位管理员不参与诚信积分体系，无需诚信证明。'
    } else {
      console.error('加载诚信证明失败:', error)
    }
  }
}

onMounted(() => {
  fetchLeaderboard()
  fetchCertificate()
})
</script>

<style scoped>
.certificate-container {
  max-width: 1300px;
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

.certificate-preview {
  background: linear-gradient(135deg, #fdf6ec, #faecd8);
  border: 3px double #e6a23c;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
}

.cert-inner h2 {
  color: #b88230;
  margin: 0 0 16px 0;
  letter-spacing: 4px;
}

.cert-name {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
  margin: 8px 0;
}

.cert-id {
  color: #909399;
  font-size: 14px;
  margin: 0 0 16px 0;
}

.cert-score {
  margin: 16px 0;
}

.score-num {
  display: block;
  font-size: 48px;
  font-weight: bold;
  color: #e6a23c;
}

.score-label {
  color: #909399;
  font-size: 13px;
}

.cert-footer {
  color: #b88230;
  font-size: 13px;
  margin: 16px 0 0 0;
}

.stats-title {
  color: #b88230;
  font-size: 13px;
  font-weight: bold;
  margin: 12px 0 4px 0;
}

.stats-item {
  display: inline-block;
  margin: 0 8px;
  font-size: 12px;
  color: #606266;
}

.record-line {
  font-size: 12px;
  color: #606266;
  margin: 2px 0;
}

.cert-actions {
  margin-top: 20px;
  text-align: center;
}

.rank-badge {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #606266;
  font-weight: bold;
  font-size: 13px;
}

.rank-badge.rank-1 {
  background: #ffd700;
  color: #fff;
}

.rank-badge.rank-2 {
  background: #c0c0c0;
  color: #fff;
}

.rank-badge.rank-3 {
  background: #cd7f32;
  color: #fff;
}
</style>
