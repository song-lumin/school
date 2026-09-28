<template>
  <div class="admin-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="数据仪表盘" name="dashboard">
        <div class="stat-cards">
          <el-card v-for="s in statCards" :key="s.label" class="stat-card">
            <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </el-card>
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card>
              <template #header>物品分类分布</template>
              <div ref="categoryChartRef" class="chart-box"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>近14日认领趋势</template>
              <div ref="trendChartRef" class="chart-box"></div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="12">
            <el-card>
              <template #header>物品流转漏斗</template>
              <div ref="funnelChartRef" class="chart-box"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>认领成功率</template>
              <div ref="rateChartRef" class="chart-box"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <el-card>
          <div style="margin-bottom: 12px">
            <el-input
              v-model="userKeyword"
              placeholder="搜索用户名/姓名/学号"
              style="width: 260px; margin-right: 12px"
              clearable
              @keyup.enter="loadUsers"
            />
            <el-button type="primary" @click="loadUsers">搜索</el-button>
          </div>
          <el-table :data="users" v-loading="usersLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="creditScore" label="诚信分" width="90" />
            <el-table-column label="角色" width="130">
              <template #default="{ row }">
                <el-tag :type="row.role === 'SYS_ADMIN' ? 'danger' : row.role === 'POINT_ADMIN' ? 'warning' : 'info'">
                  {{ row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="200">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status === 1" @click="setStatus(row, 1)">启用</el-button>
                <el-button link type="danger" :disabled="row.status === 0" @click="setStatus(row, 0)">禁用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="userPage"
            :page-size="userSize"
            :total="userTotal"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadUsers"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="申诉工单" name="disputes">
        <el-card>
          <div style="margin-bottom: 12px">
            <el-select v-model="disputeFilter.status" placeholder="状态" clearable style="width: 140px; margin-right: 12px">
              <el-option label="待处理" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-select v-model="disputeFilter.disputeType" placeholder="类型" clearable style="width: 160px; margin-right: 12px">
              <el-option label="物品不符" value="ITEM_MISMATCH" />
              <el-option label="其他纠纷" value="OTHER" />
              <el-option label="物品丢失申诉" value="ITEM_LOST" />
            </el-select>
            <el-button type="primary" @click="loadAdminDisputes">查询</el-button>
          </div>
          <el-table :data="adminDisputes" v-loading="disputesLoading" stripe>
            <el-table-column prop="id" label="工单号" width="80" />
            <el-table-column label="类型" width="130">
              <template #default="{ row }">
                <el-tag :type="DISPUTE_TYPE_MAP[row.disputeType]?.type || 'info'">
                  {{ DISPUTE_TYPE_MAP[row.disputeType]?.text || row.disputeType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申诉人" width="100" />
            <el-table-column label="关联物品" min-width="140">
              <template #default="{ row }">{{ row.itemTitle || ('#' + row.itemId) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="DISPUTE_STATUS_MAP[row.status]?.type">
                  {{ DISPUTE_STATUS_MAP[row.status]?.text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openHandleDialog(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="disputePage"
            :page-size="disputeSize"
            :total="disputeTotal"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadAdminDisputes"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="风控预警" name="risk">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>风险预警列表</span>
              <el-button type="primary" @click="loadRisk">刷新</el-button>
            </div>
          </template>
          <el-alert
            v-if="!riskWarnings.length"
            type="success"
            :closable="false"
            show-icon
            title="当前没有风险预警"
          />
          <el-table v-else :data="riskWarnings" v-loading="riskLoading" stripe>
            <el-table-column label="预警类型" width="140">
              <template #default="{ row }">
                <el-tag :type="row.warningType === 'COLLUSION' ? 'danger' : 'warning'">
                  {{ row.warningType === 'COLLUSION' ? '疑似串通' : row.warningType === 'DUPLICATE_IMAGE' ? '图片疑似重复' : '认领率异常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="涉及用户" min-width="160">
              <template #default="{ row }">{{ row.userNames.join('、') }}</template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" min-width="280" />
            <el-table-column prop="count" label="次数" width="80" />
          </el-table>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="监控调取" name="camera-logs">
        <el-card>
          <template #header>
            <div class="card-header"><span>监控调取审计记录</span><el-button type="primary" @click="openCameraCreate">新建调取申请</el-button></div>
          </template>
          <div class="filter-row">
            <el-select v-model="cameraFilter.status" clearable placeholder="全部状态" style="width: 150px">
              <el-option label="待处理" :value="0" /><el-option label="已批准" :value="1" /><el-option label="已拒绝" :value="2" /><el-option label="已完成" :value="3" />
            </el-select>
            <el-select v-model="cameraFilter.dropPointId" clearable placeholder="全部投放点" style="width: 200px">
              <el-option v-for="point in points.filter((p) => p.hasCamera === 1)" :key="point.id" :label="point.name" :value="point.id" />
            </el-select>
            <el-input-number v-model="cameraFilter.itemId" :min="1" :controls="false" placeholder="物品ID" />
            <el-button type="primary" @click="loadCameraLogs">筛选</el-button>
          </div>
          <el-table :data="cameraLogs" v-loading="cameraLoading" stripe>
            <el-table-column prop="id" label="记录号" width="85" />
            <el-table-column prop="dropPointName" label="投放点" min-width="140" />
            <el-table-column prop="itemTitle" label="关联物品" min-width="140"><template #default="{ row }">{{ row.itemTitle || (row.itemId ? `#${row.itemId}` : '未关联') }}</template></el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="applyReason" label="申请原因" min-width="180" show-overflow-tooltip />
            <el-table-column label="时间范围" min-width="180"><template #default="{ row }">{{ formatAdminTime(row.timeRangeStart) }}<br />{{ formatAdminTime(row.timeRangeEnd) }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="cameraStatusType(row.status)">{{ cameraStatusText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="resultNote" label="处理说明" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 0" link type="primary" @click="openCameraHandle(row)">处理</el-button></template></el-table-column>
          </el-table>
          <el-pagination v-model:current-page="cameraPage" :page-size="10" :total="cameraTotal" layout="total, prev, pager, next" class="pagination" @current-change="loadCameraLogs" />
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="站点管理" name="points">
        <el-card>
          <div style="margin-bottom: 12px">
            <el-button type="primary" @click="openPointDialog()">新增站点</el-button>
          </div>
          <el-table :data="points" v-loading="pointsLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="站点名称" min-width="140" />
            <el-table-column prop="location" label="位置" min-width="180" />
            <el-table-column prop="adminName" label="站点管理员" width="110">
              <template #default="{ row }">{{ row.adminName || '未分配' }}</template>
            </el-table-column>
            <el-table-column label="监控" width="90">
              <template #default="{ row }">
                <el-tag :type="row.hasCamera === 1 ? 'success' : 'info'" size="small">
                  {{ row.hasCamera === 1 ? '有' : '无' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPointDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deletePoint(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="举报审查" name="reports">
        <el-card>
          <div style="margin-bottom: 12px">
            <el-select v-model="reportFilter.status" placeholder="状态" clearable style="width: 140px; margin-right: 12px">
              <el-option label="待处理" :value="0" />
              <el-option label="举报成立" :value="1" />
              <el-option label="不成立" :value="2" />
            </el-select>
            <el-select v-model="reportFilter.reportType" placeholder="类型" clearable style="width: 140px; margin-right: 12px">
              <el-option label="虚假投放" value="FAKE_PUBLISH" />
              <el-option label="描述不符" value="DESC_MISMATCH" />
              <el-option label="其他" value="OTHER" />
            </el-select>
            <el-button type="primary" @click="loadReports">查询</el-button>
          </div>
          <el-table :data="reports" v-loading="reportsLoading" stripe>
            <el-table-column prop="id" label="举报号" width="80" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="REPORT_TYPE_MAP[row.reportType]?.type || 'info'">
                  {{ REPORT_TYPE_MAP[row.reportType]?.text || row.reportType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reporterName" label="举报人" width="100" />
            <el-table-column label="被举报物品" min-width="140">
              <template #default="{ row }">{{ row.itemTitle || ('#' + row.itemId) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="举报描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="REPORT_STATUS_MAP[row.status]?.type">
                  {{ REPORT_STATUS_MAP[row.status]?.text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="handlerNote" label="处理意见" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status !== 0" @click="openReportDialog(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="reportPage"
            :page-size="reportSize"
            :total="reportTotal"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadReports"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 申诉处理对话框 -->
    <el-dialog v-model="handleDialogVisible" title="处理申诉工单" width="620px">
      <el-descriptions v-if="handling" :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="工单号">{{ handling.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ DISPUTE_TYPE_MAP[handling.disputeType]?.text }}</el-descriptions-item>
        <el-descriptions-item label="申诉人">{{ handling.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="关联物品">{{ handling.itemTitle || ('#' + handling.itemId) }}</el-descriptions-item>
        <el-descriptions-item label="申诉描述" :span="2">{{ handling.description }}</el-descriptions-item>
        <el-descriptions-item label="证据图片" :span="2">
          <template v-if="handling.evidenceImages && handling.evidenceImages.length">
            <el-image
              v-for="(img, idx) in handling.evidenceImages"
              :key="idx"
              :src="img"
              :preview-src-list="handling.evidenceImages"
              fit="cover"
              style="width: 70px; height: 70px; margin-right: 6px"
            />
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.approved">
            <el-radio :value="true">通过（回滚积分 + 物品作废）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理意见" required>
          <el-input
            v-model="handleForm.handlerNote"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="填写处理依据和结论"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 举报处理对话框 -->
    <el-dialog v-model="reportDialogVisible" title="处理举报" width="620px">
      <el-descriptions v-if="handlingReport" :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="举报号">{{ handlingReport.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ REPORT_TYPE_MAP[handlingReport.reportType]?.text }}
        </el-descriptions-item>
        <el-descriptions-item label="举报人">{{ handlingReport.reporterName }}</el-descriptions-item>
        <el-descriptions-item label="被举报物品">
          {{ handlingReport.itemTitle || ('#' + handlingReport.itemId) }}
        </el-descriptions-item>
        <el-descriptions-item label="举报描述" :span="2">{{ handlingReport.description }}</el-descriptions-item>
        <el-descriptions-item label="证据图片" :span="2">
          <template v-if="handlingReport.evidenceImages && handlingReport.evidenceImages.length">
            <el-image
              v-for="(img, idx) in handlingReport.evidenceImages"
              :key="idx"
              :src="img"
              :preview-src-list="handlingReport.evidenceImages"
              fit="cover"
              style="width: 70px; height: 70px; margin-right: 6px"
            />
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="审查结论">
          <el-radio-group v-model="reportForm.valid">
            <el-radio :value="true">成立（物品作废 + 回滚积分）</el-radio>
            <el-radio :value="false">不成立</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reportForm.valid" label="封禁发布者">
          <el-switch v-model="reportForm.banPublisher" active-text="同时禁用发布者账号" />
        </el-form-item>
        <el-form-item label="处理意见" required>
          <el-input
            v-model="reportForm.handlerNote"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="填写审查依据和结论"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReportHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 站点编辑对话框 -->
    <el-dialog v-model="pointDialogVisible" :title="pointForm.id ? '编辑站点' : '新增站点'" width="520px">
      <el-form :model="pointForm" label-width="100px">
        <el-form-item label="站点名称" required>
          <el-input v-model="pointForm.name" maxlength="50" placeholder="如：图书馆一层服务台" />
        </el-form-item>
        <el-form-item label="位置" required>
          <el-input v-model="pointForm.location" maxlength="200" placeholder="详细位置描述" />
        </el-form-item>
        <el-form-item label="站点管理员">
          <el-input-number v-model="pointForm.adminId" :min="1" controls-position="right" placeholder="用户ID，留空则未分配" style="width: 100%" />
        </el-form-item>
        <el-form-item label="配备监控">
          <el-switch v-model="pointForm.hasCamera" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="pointForm.hasCamera === 1" label="监控信息">
          <el-input v-model="pointForm.cameraInfo" maxlength="200" placeholder="如：东门摄像头编号 CAM-012" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="pointForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPoint">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cameraCreateVisible" title="新建监控调取申请" width="560px">
      <el-form :model="cameraCreateForm" label-width="100px">
        <el-form-item label="投放点"><el-select v-model="cameraCreateForm.dropPointId" placeholder="选择已配置监控的投放点" style="width: 100%"><el-option v-for="point in points.filter((p) => p.status === 1 && p.hasCamera === 1)" :key="point.id" :label="point.name" :value="point.id" /></el-select></el-form-item>
        <el-form-item label="关联物品"><el-input-number v-model="cameraCreateForm.itemId" :min="1" :controls="false" placeholder="可留空" /></el-form-item>
        <el-form-item label="申请原因"><el-input v-model="cameraCreateForm.applyReason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="cameraCreateForm.timeRangeStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="cameraCreateForm.timeRangeEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="cameraCreateVisible = false">取消</el-button><el-button type="primary" :loading="cameraSubmitting" @click="submitCameraCreate">提交申请</el-button></template>
    </el-dialog>
    <el-dialog v-model="cameraHandleVisible" title="处理监控调取申请" width="520px">
      <el-radio-group v-model="cameraHandleForm.status"><el-radio-button :value="1">批准调取</el-radio-button><el-radio-button :value="2">拒绝申请</el-radio-button></el-radio-group>
      <el-input v-model="cameraHandleForm.resultNote" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审批依据或调取结果" style="margin-top: 18px" />
      <template #footer><el-button @click="cameraHandleVisible = false">取消</el-button><el-button type="primary" :loading="cameraSubmitting" @click="submitCameraHandle">确认处理</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { adminApi, disputeApi, riskControlApi, reportApi, dropPointAdminApi, dropPointApi, cameraLogAdminApi } from '@/api'
import type {
  DisputeVO,
  RiskWarningVO,
  DashboardVO,
  User,
  ReportVO,
  DropPoint
} from '@/types'
import type { CameraLog } from '@/api'
import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP, REPORT_TYPE_MAP, REPORT_STATUS_MAP } from '@/types'

const activeTab = ref('dashboard')
const cameraLogs = ref<CameraLog[]>([])
const cameraLoading = ref(false)
const cameraSubmitting = ref(false)
const cameraPage = ref(1)
const cameraTotal = ref(0)
const cameraFilter = ref<{ status?: number; dropPointId?: number; itemId?: number }>({})
const cameraCreateVisible = ref(false)
const cameraHandleVisible = ref(false)
const cameraHandleId = ref<number | null>(null)
const cameraCreateForm = ref({ dropPointId: undefined as number | undefined, itemId: undefined as number | undefined, applyReason: '', timeRangeStart: '', timeRangeEnd: '' })
const cameraHandleForm = ref({ status: 1, resultNote: '' })

// ===== 仪表盘 =====
const dashboard = ref<DashboardVO | null>(null)
const categoryChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
const funnelChartRef = ref<HTMLElement>()
const rateChartRef = ref<HTMLElement>()

const statCards = computed(() => {
  const d = dashboard.value
  if (!d) return []
  return [
    { label: '物品总数', value: d.totalItems, color: '#409eff' },
    { label: '公开待认领', value: d.publicItems, color: '#67c23a' },
    { label: '成功领取', value: d.pickedUpItems, color: '#e6a23c' },
    { label: '注册用户', value: d.totalUsers, color: '#909399' },
    { label: '认领申请', value: d.totalClaims, color: '#f56c6c' },
    { label: '认领成功', value: d.successClaims, color: '#67c23a' }
  ]
})

const renderCharts = () => {
  const d = dashboard.value
  if (!d) return

  // 分类饼图
  if (categoryChartRef.value) {
    const chart = echarts.init(categoryChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '68%'],
          label: { formatter: '{b}: {c}' },
          data: Object.entries(d.itemsByCategory).map(([name, value]) => ({ name, value }))
        }
      ]
    })
  }

  // 趋势折线图
  if (trendChartRef.value) {
    const chart = echarts.init(trendChartRef.value)
    const entries = Object.entries(d.claimsByDay).sort(([a], [b]) => a.localeCompare(b))
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: entries.map(([k]) => k.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.15 },
          data: entries.map(([, v]) => v),
          itemStyle: { color: '#409eff' }
        }
      ]
    })
  }

  // 流转漏斗
  if (funnelChartRef.value) {
    const chart = echarts.init(funnelChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [
        {
          type: 'funnel',
          left: '10%',
          label: { formatter: '{b}: {c}' },
          data: [
            { name: '发布物品', value: d.totalItems },
            { name: '公开中', value: d.publicItems },
            { name: '认领申请', value: d.totalClaims },
            { name: '成功领取', value: d.pickedUpItems }
          ]
        }
      ]
    })
  }

  // 认领成功率仪表盘
  if (rateChartRef.value) {
    const chart = echarts.init(rateChartRef.value)
    const rate = d.totalClaims > 0 ? Math.round((d.successClaims / d.totalClaims) * 100) : 0
    chart.setOption({
      series: [
        {
          type: 'gauge',
          progress: { show: true, width: 14 },
          axisLine: { lineStyle: { width: 14 } },
          detail: { formatter: '{value}%', fontSize: 22 },
          data: [{ value: rate, name: '认领成功率' }],
          title: { fontSize: 13 }
        }
      ]
    })
  }
}

const loadDashboard = async () => {
  const res = await adminApi.getDashboard()
  dashboard.value = res.data
  await nextTick()
  renderCharts()
}

// ===== 用户管理 =====
const users = ref<User[]>([])
const usersLoading = ref(false)
const userKeyword = ref('')
const userPage = ref(1)
const userSize = ref(10)
const userTotal = ref(0)

const loadUsers = async () => {
  usersLoading.value = true
  try {
    const res = await adminApi.listUsers({
      keyword: userKeyword.value || undefined,
      page: userPage.value,
      size: userSize.value
    })
    users.value = res.data.records
    userTotal.value = res.data.total
  } finally {
    usersLoading.value = false
  }
}

const setStatus = async (row: User, status: number) => {
  const { default: request } = await import('@/utils/request')
  await request.put(`/admin/users/${row.id}/status`, { status })
  ElMessage.success('操作成功')
  await loadUsers()
}

// ===== 申诉工单 =====
const adminDisputes = ref<DisputeVO[]>([])
const disputesLoading = ref(false)
const disputeFilter = ref<{ status?: number; disputeType?: string }>({})
const disputePage = ref(1)
const disputeSize = ref(10)
const disputeTotal = ref(0)
const handleDialogVisible = ref(false)
const handling = ref<DisputeVO | null>(null)
const handleForm = ref({ approved: true, handlerNote: '' })
const submitting = ref(false)

const loadAdminDisputes = async () => {
  disputesLoading.value = true
  try {
    const res = await disputeApi.list({
      status: disputeFilter.value.status,
      disputeType: disputeFilter.value.disputeType,
      page: disputePage.value,
      size: disputeSize.value
    })
    adminDisputes.value = res.data.records
    disputeTotal.value = res.data.total
  } finally {
    disputesLoading.value = false
  }
}

const openHandleDialog = (row: DisputeVO) => {
  handling.value = row
  handleForm.value = { approved: true, handlerNote: '' }
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  if (!handling.value) return
  if (!handleForm.value.handlerNote.trim()) {
    ElMessage.warning('请填写处理意见')
    return
  }
  submitting.value = true
  try {
    await disputeApi.handle(handling.value.id, {
      approved: handleForm.value.approved,
      handlerNote: handleForm.value.handlerNote.trim()
    })
    ElMessage.success(handleForm.value.approved ? '已通过：积分已回滚，物品已作废' : '已驳回')
    handleDialogVisible.value = false
    await loadAdminDisputes()
  } finally {
    submitting.value = false
  }
}

// ===== 风控预警 =====
const riskWarnings = ref<RiskWarningVO[]>([])
const riskLoading = ref(false)

const loadRisk = async () => {
  riskLoading.value = true
  try {
    const res = await riskControlApi.getWarnings()
    riskWarnings.value = res.data
  } finally {
    riskLoading.value = false
  }
}

const loadCameraLogs = async () => {
  cameraLoading.value = true
  try {
    const res = await cameraLogAdminApi.list({ ...cameraFilter.value, page: cameraPage.value, size: 10 })
    cameraLogs.value = res.data.records
    cameraTotal.value = Number(res.data.total)
  } finally { cameraLoading.value = false }
}

const openCameraCreate = async () => {
  if (!points.value.length) await loadPoints()
  cameraCreateForm.value = { dropPointId: undefined, itemId: undefined, applyReason: '', timeRangeStart: '', timeRangeEnd: '' }
  cameraCreateVisible.value = true
}

const submitCameraCreate = async () => {
  const form = cameraCreateForm.value
  if (!form.dropPointId || !form.applyReason.trim() || !form.timeRangeStart || !form.timeRangeEnd) {
    ElMessage.warning('请完整填写投放点、原因和时间范围')
    return
  }
  cameraSubmitting.value = true
  try {
    await cameraLogAdminApi.create({ ...form, applyReason: form.applyReason.trim() })
    ElMessage.success('调取申请已记录')
    cameraCreateVisible.value = false
    await loadCameraLogs()
  } finally { cameraSubmitting.value = false }
}

const openCameraHandle = (row: CameraLog) => {
  cameraHandleId.value = row.id
  cameraHandleForm.value = { status: 1, resultNote: '' }
  cameraHandleVisible.value = true
}

const submitCameraHandle = async () => {
  if (!cameraHandleId.value || !cameraHandleForm.value.resultNote.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  cameraSubmitting.value = true
  try {
    await cameraLogAdminApi.handle(cameraHandleId.value, { ...cameraHandleForm.value, resultNote: cameraHandleForm.value.resultNote.trim() })
    ElMessage.success('监控申请已处理')
    cameraHandleVisible.value = false
    await loadCameraLogs()
  } finally { cameraSubmitting.value = false }
}

const formatAdminTime = (time?: string | null) => time ? time.replace('T', ' ').slice(0, 16) : '-'
const cameraStatusText = (status: number) => ['待处理', '已批准', '已拒绝', '已完成'][status] || '未知'
const cameraStatusType = (status: number) => status === 1 ? 'success' : status === 2 ? 'danger' : status === 0 ? 'warning' : 'info'

// ===== 站点管理 =====
const points = ref<DropPoint[]>([])
const pointsLoading = ref(false)
const pointDialogVisible = ref(false)
const pointForm = ref<{ id?: number; name: string; location: string; adminId?: number; hasCamera: number; cameraInfo?: string; status: number }>({
  name: '',
  location: '',
  hasCamera: 0,
  status: 1
})

const loadPoints = async () => {
  pointsLoading.value = true
  try {
    const res = await dropPointApi.list()
    points.value = res.data
  } finally {
    pointsLoading.value = false
  }
}

const openPointDialog = (row?: DropPoint) => {
  pointForm.value = row
    ? { id: row.id, name: row.name, location: row.location, adminId: row.adminId ?? undefined, hasCamera: row.hasCamera, cameraInfo: row.cameraInfo, status: row.status }
    : { name: '', location: '', hasCamera: 0, status: 1 }
  pointDialogVisible.value = true
}

const submitPoint = async () => {
  if (!pointForm.value.name.trim() || !pointForm.value.location.trim()) {
    ElMessage.warning('请填写站点名称和位置')
    return
  }
  submitting.value = true
  try {
    if (pointForm.value.id) {
      await dropPointAdminApi.update(pointForm.value.id, pointForm.value)
      ElMessage.success('站点已更新')
    } else {
      await dropPointAdminApi.create(pointForm.value)
      ElMessage.success('站点已创建')
    }
    pointDialogVisible.value = false
    await loadPoints()
  } finally {
    submitting.value = false
  }
}

const deletePoint = async (row: DropPoint) => {
  try {
    await ElMessageBox.confirm(`确定删除站点「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await dropPointAdminApi.delete(row.id)
  ElMessage.success('已删除')
  await loadPoints()
}

// ===== 举报审查 =====
const reports = ref<ReportVO[]>([])
const reportsLoading = ref(false)
const reportFilter = ref<{ status?: number; reportType?: string }>({})
const reportPage = ref(1)
const reportSize = ref(10)
const reportTotal = ref(0)
const reportDialogVisible = ref(false)
const handlingReport = ref<ReportVO | null>(null)
const reportForm = ref({ valid: true, handlerNote: '', banPublisher: false })

const loadReports = async () => {
  reportsLoading.value = true
  try {
    const res = await reportApi.list({
      status: reportFilter.value.status,
      reportType: reportFilter.value.reportType,
      page: reportPage.value,
      size: reportSize.value
    })
    reports.value = res.data.records
    reportTotal.value = res.data.total
  } finally {
    reportsLoading.value = false
  }
}

const openReportDialog = (row: ReportVO) => {
  handlingReport.value = row
  reportForm.value = { valid: true, handlerNote: '', banPublisher: false }
  reportDialogVisible.value = true
}

const submitReportHandle = async () => {
  if (!handlingReport.value) return
  if (!reportForm.value.handlerNote.trim()) {
    ElMessage.warning('请填写处理意见')
    return
  }
  submitting.value = true
  try {
    await reportApi.handle(handlingReport.value.id, {
      valid: reportForm.value.valid,
      handlerNote: reportForm.value.handlerNote.trim(),
      banPublisher: reportForm.value.valid ? reportForm.value.banPublisher : undefined
    })
    ElMessage.success(reportForm.value.valid ? '举报成立：物品已作废，积分已回滚' : '已标记不成立')
    reportDialogVisible.value = false
    await loadReports()
  } finally {
    submitting.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'users' && !users.value.length) loadUsers()
  if (tab === 'disputes' && !adminDisputes.value.length) loadAdminDisputes()
  if (tab === 'risk' && !riskWarnings.value.length) loadRisk()
  if (tab === 'camera-logs' && !cameraLogs.value.length) { loadPoints(); loadCameraLogs() }
  if (tab === 'points' && !points.value.length) loadPoints()
  if (tab === 'reports' && !reports.value.length) loadReports()
})

onMounted(loadDashboard)
</script>

<style scoped>
.admin-container {
  max-width: 1400px;
  margin: 0 auto;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.chart-box {
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
