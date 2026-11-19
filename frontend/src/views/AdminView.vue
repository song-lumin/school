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
          <el-tabs v-model="userRoleTab">
            <el-tab-pane label="普通用户" name="USER" />
            <el-tab-pane label="站点管理员" name="POINT_ADMIN" />
            <el-tab-pane label="系统管理员" name="SYS_ADMIN" />
          </el-tabs>

          <div style="margin-bottom: 12px">
            <el-input
              v-model="userKeyword"
              placeholder="搜索用户名/姓名/学号"
              style="width: 260px; margin-right: 12px"
              clearable
              @keyup.enter="searchUsers"
            />
            <el-button type="primary" @click="searchUsers">搜索</el-button>
          </div>
          <el-table :data="users" v-loading="usersLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column v-if="userRoleTab !== 'SYS_ADMIN'" prop="creditScore" label="诚信分" width="90" />
            <el-table-column v-if="userRoleTab === 'POINT_ADMIN'" label="负责站点" min-width="200">
              <template #default="{ row }">
                <template v-if="pointsOf(row.id).length">
                  <el-tag v-for="p in pointsOf(row.id)" :key="p.id" size="small" style="margin-right: 6px">{{ p.name }}</el-tag>
                </template>
                <span v-else class="unassigned">未分配站点</span>
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

      <el-tab-pane label="申诉工单" name="disputes">
        <el-card>
          <div style="margin-bottom: 12px">
            <el-select v-model="disputeFilter.status" placeholder="状态" clearable style="width: 140px; margin-right: 12px">
              <el-option label="未处理" :value="0" />
              <el-option label="处理中（站点核验中）" :value="3" />
              <el-option label="待积分处置（站点已给结论）" :value="4" />
              <el-option label="已结案 · 通过" :value="1" />
              <el-option label="已结案 · 驳回" :value="2" />
            </el-select>
            <el-select v-model="disputeFilter.disputeType" placeholder="类型" clearable style="width: 160px; margin-right: 12px">
              <el-option label="物品不符" value="ITEM_MISMATCH" />
              <el-option label="物品损坏" value="ITEM_DAMAGED" />
              <el-option label="找不到物品" value="ITEM_NOT_FOUND" />
              <el-option label="其他纠纷" value="OTHER" />
              <el-option label="物品被冒领" value="FALSE_CLAIM" />
            </el-select>
            <el-button type="primary" @click="loadAdminDisputes">查询</el-button>
            <el-button @click="triggerEscalate" :loading="escalating">立即跑一次站点催办</el-button>
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
            <el-table-column label="站点核验" width="140">
              <template #default="{ row }">
                <el-tag v-if="row.pointCheckedAt" size="small" type="success">已回填</el-tag>
                <el-tag v-else-if="row.pointSlaOverdue" size="small" type="danger">超时未响应</el-tag>
                <el-tag v-else size="small" type="warning">待核验</el-tag>
                <div v-if="!row.pointCheckedAt && row.pointRemindCount > 0" class="sub-meta">
                  已催办 {{ row.pointRemindCount }} 次
                </div>
                <div v-if="row.siteUnresponsive === 1" class="sub-meta danger-text">
                  已标记站点未响应
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="DISPUTE_STATUS_MAP[row.status]?.type">
                  {{ DISPUTE_STATUS_MAP[row.status]?.text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 4" link type="primary" @click="openDisputeCredit(row)">
                  处置积分
                </el-button>
                <el-button v-else link type="primary" @click="openHandleDialog(row)">处理</el-button>
                <el-button link type="warning" @click="openChat(row)">沟通</el-button>
                <el-tag v-if="row.status === 4" size="small" type="warning" style="margin-left:6px">待处置</el-tag>
                <el-tag v-else-if="row.status === 3" size="small" type="primary" style="margin-left:6px">待定责</el-tag>
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
                <el-tag :type="row.warningType === 'COLLUSION' || row.warningType === 'MISCLAIM_FREQUENT' ? 'danger' : 'warning'">
                  {{ row.warningType === 'COLLUSION' ? '疑似串通'
                    : row.warningType === 'DUPLICATE_IMAGE' ? '图片疑似重复'
                    : row.warningType === 'MISCLAIM_FREQUENT' ? '高频误领' : '认领率异常' }}
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
      <el-tab-pane label="积分管理" name="credits">
        <el-card>
          <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center">
            <el-input v-model="creditKeyword" placeholder="搜索用户名/姓名" style="width:220px" clearable @keyup.enter="loadCreditBoard" />
            <el-button type="primary" @click="loadCreditBoard">查询</el-button>
          </div>
          <el-table :data="creditBoard" v-loading="creditLoading" stripe max-height="600">
            <el-table-column prop="rank" label="排名" width="70" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="creditScore" label="当前积分" width="100" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="openCreditLogDialog(row)">台账</el-button>
                <el-button size="small" @click="openCreditDialog(row)">调整</el-button>
                <el-button size="small" type="danger" plain @click="clearCredit(row)">清零</el-button>
              </template>
            </el-table-column>
          </el-table>
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
            <el-table-column label="联系电话" width="130">
              <template #default="{ row }">
                <span v-if="row.adminPhone" class="station-phone">{{ row.adminPhone }}</span>
                <el-tag v-else size="small" type="danger" effect="plain">未登记</el-tag>
              </template>
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



    </el-tabs>

    <el-dialog v-model="creditLogVisible" title="积分台账" width="700px">
      <p style="margin-bottom:10px">
        <b>{{ creditLogUser.realName || creditLogUser.username }}</b>（{{ creditLogUser.username }}）
        当前积分：{{ creditLogUser.creditScore }}
      </p>
      <el-table :data="creditLogs" v-loading="creditLogLoading" size="small" stripe max-height="400">
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt?.replace('T',' ').slice(0,16) }}</template>
        </el-table-column>
        <el-table-column prop="operationType" label="类型" width="140" />
        <el-table-column prop="changeAmount" label="变动" width="80">
          <template #default="{ row }">
            <span :style="{color: row.changeAmount >= 0 ? '#67c23a' : '#f56c6c'}">
              {{ row.changeAmount >= 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="说明" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
    <el-dialog v-model="creditDialogVisible" title="调整积分" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ creditForm.realName }}（{{ creditForm.username }}）</span>
        </el-form-item>
        <el-form-item label="当前积分">
          <span>{{ creditForm.currentScore }}</span>
        </el-form-item>
        <el-form-item label="新积分">
          <el-input-number v-model="creditForm.newScore" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="creditForm.reason" type="textarea" :rows="2" placeholder="必填，记录调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreditAdjust">确定</el-button>
      </template>
    </el-dialog>

    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 申诉人与站点负责人" />

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
        <el-descriptions-item label="投放点" :span="2">
          <span v-if="handling.dropPointName">{{ handling.dropPointName }}</span>
          <span v-else>未关联投放点</span>
          <span v-if="handling.dropPointAdminName" class="station-phone">
            · 站点负责人 {{ handling.dropPointAdminName }}
            <template v-if="handling.dropPointAdminPhone"> {{ handling.dropPointAdminPhone }}</template>
            <template v-else>（未登记电话）</template>
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="现场核验" :span="2">
          <div v-if="handling.pointCheckNote">
            <div class="point-check-note">{{ handling.pointCheckNote }}</div>
            <div class="point-check-meta">
              {{ handling.pointCheckerName || '站点管理员' }}
              <span v-if="handling.pointCheckStandIn" class="standin-tag">代填</span>
              · {{ (handling.pointCheckedAt || '').replace('T', ' ').slice(0, 16) }}
            </div>
            <div v-if="handling.pointCheckStandIn" class="point-check-meta">
              站点负责人未在岗，本条由系统管理员电话核实后代填，已同步通知站点负责人留痕。
            </div>
          </div>
          <div v-else class="point-check-missing-wrap">
            <div class="point-check-missing">
              <el-tag :type="handling.pointSlaOverdue ? 'danger' : 'warning'" size="small">
                {{ handling.pointSlaOverdue ? '超 24h 未响应' : '站点尚未回填' }}
              </el-tag>
              <span>
                已等待 {{ handling.pendingHours ?? 0 }} 小时，线下事实未核验。
                站点是兼职岗、不一定在岗：建议先电话联系负责人；
                联系不上可自行核实后代填，结论里写明核实方式。
              </span>
            </div>
            <div v-if="(handling.pointRemindCount ?? 0) > 0" class="sla-line">
              系统已催办 {{ handling.pointRemindCount }} 次，最近一次
              {{ (handling.pointEscalatedAt || '').replace('T', ' ').slice(0, 16) }}。
            </div>
            <div v-if="handling.siteUnresponsive === 1" class="sla-line danger-text">
              本单已标记「站点未响应」（{{ (handling.siteUnresponsiveAt || '').replace('T', ' ').slice(0, 16) }}）。
            </div>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-collapse v-if="handling && !handling.pointCheckNote" v-model="standInCollapse" style="margin-bottom: 14px">
        <el-collapse-item name="standin">
          <template #title>
            <span class="standin-title">站点不在岗？由我核实后代填现场事实</span>
          </template>
          <el-input
            v-model="standInNote"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="例：10:35 致电图书馆服务台，负责人今日休班，由当班同学代为查找，货架 A 区未见该物品，该区监控可用。"
          />
          <div class="standin-actions">
            <span class="standin-hint">代填会以你的账号留痕，并自动通知站点负责人「本条已由系统代填」。</span>
            <el-button type="primary" size="small" :loading="standInSubmitting" @click="submitStandIn">代填现场核验</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-alert
        v-if="handling?.disputeType === 'ITEM_NOT_FOUND'"
        type="info"
        :closable="false"
        style="margin-bottom: 14px"
      >
        <div v-if="handlingPoint">
          核查站点：<b>{{ handlingPoint.name }}</b>
          <span v-if="handlingPoint.location">（{{ handlingPoint.location }}）</span>
          <span v-if="handlingPoint.adminPhone" class="station-phone">
            · 负责人 {{ handlingPoint.adminName || '站点管理员' }} {{ handlingPoint.adminPhone }}
          </span>
          <span v-else style="color: #909399">· 站点电话未登记，请到服务站现场核实</span>
        </div>
        <div v-else>正在读取站点信息…</div>
        <div style="margin-top: 4px; color: #606266; font-size: 12px">
          下结论前先拨打站点电话确认实物是否在架，再据实选择三种结论。
        </div>
      </el-alert>

      <el-alert
        v-if="handling && !handling.pointCheckNote"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
        title="站点仍未回填现场核验。确认处理后，系统会自动把本单标记为「站点未响应」并记录标记时间，标记不可撤销。"
      />

      <el-form label-width="90px">
        <el-form-item label="处理结论">
          <el-radio-group v-model="handleForm.approved">
            <el-radio :value="true">通过（申诉成立，按下面勾选回滚）</el-radio>
            <el-radio :value="false">驳回（不成立，不回滚任何积分）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回滚积分">
          <el-checkbox-group v-model="handleForm.rollbackItems" :disabled="!handleForm.approved" class="rollback-group">
            <el-checkbox value="PUBLISH_CHECK">招领方 · 投放分 +1（发布招领时发放的 1 分）</el-checkbox>
            <el-checkbox value="PUBLISH_PICKUP">招领方 · 认领分 +3（物品被领取时发放的 3 分）</el-checkbox>
            <el-checkbox value="FORWARD">转发方 · 转发分 +1（转发这条招领发放的 1 分）</el-checkbox>
            <el-checkbox value="CLAIMER">认领方 · 认领分 +1（认领人取件时发放的 1 分）</el-checkbox>
          </el-checkbox-group>
          <div class="form-hint">
            只勾选本次要撤回的分；没有真实发放过的项，系统自动跳过，不会扣成负数。
          </div>
        </el-form-item>
        <el-form-item label="物品处置">
          <el-select v-model="handleForm.itemDispose" style="width: 300px">
            <el-option label="不改动（只处理积分）" value="NONE" />
            <el-option label="作废归档（不再重新公开）" value="VOIDED" />
            <el-option label="转「丢失待查」" value="LOST" />
            <el-option label="转「待归还」（站点收回后重新公开）" value="RETURNING" />
            <el-option label="重新公开（回公开池待认领）" value="PUBLIC" />
          </el-select>
          <div class="form-hint">不改物品状态就只动积分；要收回实物请选「待归还」或「作废归档」。</div>
        </el-form-item>
        <el-form-item label="回滚原因" required>
          <el-input
            v-model="handleForm.handlerNote"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="写明为什么这样回滚：依据哪条规则、站点反馈了什么、证据是什么"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 积分处置对话框：站点已给结论，系统只执行积分 -->
    <el-dialog v-model="disputeCreditVisible" title="积分处置（站点已结案）" width="700px">
      <el-descriptions v-if="handling" :column="2" border style="margin-bottom: 12px">
        <el-descriptions-item label="工单号">{{ handling.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ DISPUTE_TYPE_MAP[handling.disputeType]?.text }}</el-descriptions-item>
        <el-descriptions-item label="申诉人">{{ handling.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="关联物品">{{ handling.itemTitle || ('#' + handling.itemId) }}</el-descriptions-item>
        <el-descriptions-item label="站点结论" :span="2">
          <el-tag type="success">{{ handling.siteVerdictText || handling.siteVerdict }}</el-tag>
          <el-tag v-if="handling.needReview === 1" type="danger" style="margin-left: 6px">涉及站点责任/不可抗力 · 需复核</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="站点建议物品处置" :span="2">
          {{ DISPOSE_LABELS[handling.siteDispose || 'NONE'] || handling.siteDispose }}
        </el-descriptions-item>
        <el-descriptions-item label="站点建议回滚" :span="2">
          {{ handling.siteSuggestItems
            ? handling.siteSuggestItems.split(',').map(i => CREDIT_ITEM_LABELS[i] || i).join('、')
            : '无需回滚' }}
          <div v-if="creditExtraPunish" class="form-hint">附加处罚：{{ creditExtraPunish }}（执行时一并生效）</div>
        </el-descriptions-item>
        <el-descriptions-item label="站点现场说明" :span="2">
          <div class="point-check-note">{{ handling.pointCheckNote || '站点未填写' }}</div>
          <div class="point-check-meta">
            {{ handling.pointCheckerName || '站点管理员' }}
            <span v-if="handling.pointCheckStandIn" class="standin-tag">代填</span>
            · {{ (handling.pointCheckedAt || '').replace('T', ' ').slice(0, 16) }}
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="系统只做积分与处罚：按站点结论执行、或按你的判断调整回滚项、或打回站点补充事实。事实认定归站点，系统不重新判断实物与监控。"
      />

      <el-form label-width="90px">
        <el-form-item label="处置方式">
          <el-radio-group v-model="disputeCreditForm.action" style="display:flex;flex-direction:column;gap:6px">
            <el-radio value="EXECUTE">全部执行 · 按站点结论自动回滚（推荐）</el-radio>
            <el-radio value="ADJUST">逐条调整 · 我自己勾选要回滚的分</el-radio>
            <el-radio value="REJECT">打回站点重判 · 不动积分，退回「处理中」</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="disputeCreditForm.action === 'ADJUST'">
          <el-form-item label="回滚积分">
            <el-checkbox-group v-model="disputeCreditForm.rollbackItems" class="rollback-group">
              <el-checkbox value="PUBLISH_CHECK">招领方 · 投放分 +1</el-checkbox>
              <el-checkbox value="PUBLISH_PICKUP">招领方 · 认领分 +3</el-checkbox>
              <el-checkbox value="FORWARD">转发方 · 转发分 +1</el-checkbox>
              <el-checkbox value="CLAIMER">认领方 · 认领分 +1</el-checkbox>
            </el-checkbox-group>
            <div class="form-hint">没真实发放过的项系统自动跳过，不会扣成负数。</div>
          </el-form-item>
          <el-form-item label="物品处置">
            <el-select v-model="disputeCreditForm.itemDispose" style="width: 320px">
              <el-option label="不改动（只处理积分）" value="NONE" />
              <el-option label="待归还（站点收回，物品完好才重新公开）" value="RETURNING" />
              <el-option label="作废归档（不再重新公开）" value="VOIDED" />
              <el-option label="转「丢失待查」" value="LOST" />
              <el-option label="重新公开（回公开池待认领）" value="PUBLIC" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="处置说明" required>
          <el-input
            v-model="disputeCreditForm.note"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="写清为什么这样处置：按站点结论执行，或调整了哪一项、为什么；打回时写明还缺什么事实。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeCreditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDisputeCredit">确认处置</el-button>
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
import { adminApi, disputeApi, riskControlApi, reportApi, dropPointAdminApi, dropPointApi, cameraLogAdminApi, creditApi, foundItemApi } from '@/api'
import type {
  DisputeVO,
  RiskWarningVO,
  DashboardVO,
  User,
  ReportVO,
  DropPoint
} from '@/types'
import type { CameraLog } from '@/api'
import { DISPUTE_TYPE_MAP, DISPUTE_STATUS_MAP, REPORT_TYPE_MAP, REPORT_STATUS_MAP, CREDIT_ITEM_LABELS, DISPOSE_LABELS, DISPUTE_VERDICT_OPTIONS } from '@/types'
import ChatDialog from '@/components/ChatDialog.vue'

const activeTab = ref('dashboard')

const cameraLogs = ref<CameraLog[]>([])
const creditBoard = ref<any[]>([])
const creditLoading = ref(false)
const creditKeyword = ref('')
const creditLogVisible = ref(false)
const creditLogLoading = ref(false)
const creditLogUser = ref<any>({})
const creditLogs = ref<any[]>([])
const openCreditLogDialog = async (row: any) => {
  creditLogUser.value = row
  creditLogVisible.value = true
  creditLogLoading.value = true
  creditLogs.value = []
  try {
    const res = await adminApi.userCreditLogs(row.userId || row.id)
    creditLogs.value = res.data?.records || res.data || []
  } catch (e) { console.error(e) } finally { creditLogLoading.value = false }
}
const creditDialogVisible = ref(false)
const creditForm = ref<any>({ userId: null, realName: '', username: '', currentScore: 0, newScore: 0, reason: '' })
const openCreditDialog = (row: any) => {
  creditForm.value = { userId: row.userId || row.id, realName: row.realName, username: row.username, currentScore: row.creditScore, newScore: row.creditScore, reason: '' }
  creditDialogVisible.value = true
}
const submitCreditAdjust = async () => {
  if (!creditForm.value.reason?.trim()) { ElMessage.warning('请填写调整原因'); return }
  try {
    await adminApi.adjustCredit(creditForm.value.userId, { newScore: creditForm.value.newScore, reason: creditForm.value.reason })
    ElMessage.success('积分已调整')
    creditDialogVisible.value = false
    loadCreditBoard()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '调整失败') }
}
const clearCredit = (row: any) => {
  creditForm.value = { userId: row.userId || row.id, realName: row.realName, username: row.username, currentScore: row.creditScore, newScore: 0, reason: '' }
  ElMessageBox.prompt('清零该用户积分，请填写原因：', '清零积分', { inputPlaceholder: '必填' })
    .then(({ value }) => {
      if (!value?.trim()) throw new Error('empty')
      return adminApi.adjustCredit(creditForm.value.userId, { newScore: 0, reason: value })
    })
    .then(() => { ElMessage.success('已清零'); loadCreditBoard() })
    .catch(() => {})
}
const loadCreditBoard = async () => {
  creditLoading.value = true
  try {
    const res = await creditApi.getLeaderboard(500)
    let list = res.data.entries || []
    if (creditKeyword.value) {
      const k = creditKeyword.value.toLowerCase()
      list = list.filter((u: any) =>
        (u.realName || '').toLowerCase().includes(k) ||
        (u.username || '').toLowerCase().includes(k))
    }
    creditBoard.value = list.map((u: any, i: number) => ({ ...u, rank: i + 1 }))
  } catch (e) { console.error(e) } finally { creditLoading.value = false }
}
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
const userRoleTab = ref<'USER' | 'POINT_ADMIN' | 'SYS_ADMIN'>('USER')
const userPage = ref(1)
const userSize = ref(10)
const userTotal = ref(0)

const loadUsers = async () => {
  usersLoading.value = true
  try {
    const res = await adminApi.listUsers({
      keyword: userKeyword.value || undefined,
      role: userRoleTab.value,
      page: userPage.value,
      size: userSize.value
    })
    users.value = res.data.records
    userTotal.value = res.data.total
  } finally {
    usersLoading.value = false
  }
}

const searchUsers = () => {
  userPage.value = 1
  loadUsers()
}

const pointsOf = (adminId: number) => points.value.filter((p) => p.adminId === adminId)

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
const handlingPoint = ref<{ name: string; location?: string; adminName?: string; adminPhone?: string } | null>(null)
const handleForm = ref<{ approved: boolean; rollbackItems: string[]; itemDispose: string; handlerNote: string }>(
  { approved: true, rollbackItems: [], itemDispose: 'NONE', handlerNote: '' }
)
const submitting = ref(false)

// 积分处置：站点已出结论（status=4）的工单，系统只执行积分
const disputeCreditVisible = ref(false)
const disputeCreditForm = ref<{
  action: 'EXECUTE' | 'ADJUST' | 'REJECT'
  rollbackItems: string[]
  itemDispose: string
  note: string
}>({ action: 'EXECUTE', rollbackItems: [], itemDispose: 'NONE', note: '' })

/** 结论附带的固定处罚，直接从选项表取，避免前后端口径漂移 */
const creditExtraPunish = computed(() => {
  const t = handling.value?.disputeType || ''
  const v = handling.value?.siteVerdict || ''
  const opts = (DISPUTE_VERDICT_OPTIONS as any)[t] || []
  const hit = opts.find((o: any) => o.value === v)
  return (hit && hit.extra) || ''
})

const openDisputeCredit = (row: DisputeVO) => {
  handling.value = row
  const suggest = (row.siteSuggestItems || '').split(',').filter(Boolean)
  disputeCreditForm.value = {
    action: 'EXECUTE',
    rollbackItems: suggest,
    itemDispose: row.siteDispose || 'NONE',
    note: ''
  }
  disputeCreditVisible.value = true
}

const submitDisputeCredit = async () => {
  if (!handling.value) return
  if (!disputeCreditForm.value.note.trim()) { ElMessage.warning('请填写处置说明'); return }
  submitting.value = true
  try {
    await disputeApi.applyCredit(handling.value.id, {
      action: disputeCreditForm.value.action,
      ...(disputeCreditForm.value.action === 'ADJUST'
        ? { rollbackItems: disputeCreditForm.value.rollbackItems, itemDispose: disputeCreditForm.value.itemDispose }
        : {}),
      note: disputeCreditForm.value.note.trim()
    })
    ElMessage.success(disputeCreditForm.value.action === 'REJECT'
      ? '已打回站点重判，站点负责人会收到通知'
      : '积分处置已完成，站点与申诉人已收到回执')
    disputeCreditVisible.value = false
    await loadAdminDisputes()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '处置失败')
  } finally {
    submitting.value = false
  }
}

// 站点回填 SLA：手动触发一次催办扫描（定时任务每小时也会跑）
const escalating = ref(false)
const triggerEscalate = async () => {
  escalating.value = true
  try {
    const res = await disputeApi.escalate()
    const n = Number(res.data ?? 0)
    ElMessage.success(n > 0
      ? `已生成 ${n} 条催办任务，可在消息中心查看`
      : '暂无需催办的工单（未满 24h、已回填或已催满 3 次）')
    await loadAdminDisputes()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '催办失败')
  } finally {
    escalating.value = false
  }
}

// 站点不在岗时的兜底：由系统管理员电话核实后代填现场核验
const standInCollapse = ref<string[]>([])
const standInNote = ref('')
const standInSubmitting = ref(false)
const submitStandIn = async () => {
  if (!handling.value) return
  if (!standInNote.value.trim()) { ElMessage.warning('请填写核实到的事实'); return }
  standInSubmitting.value = true
  try {
    await disputeApi.pointCheck(handling.value.id, standInNote.value.trim())
    ElMessage.success('已代填现场核验，站点负责人已收到留痕通知')
    standInNote.value = ''
    standInCollapse.value = []
    const res = await disputeApi.getById(handling.value.id)
    handling.value = res.data
    await loadAdminDisputes()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '代填失败')
  } finally {
    standInSubmitting.value = false
  }
}

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

const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const openHandleDialog = (row: DisputeVO) => {
  handling.value = row
  handleForm.value = {
    approved: true,
    rollbackItems: [],
    itemDispose: 'NONE',
    handlerNote: ''
  }
  standInNote.value = ''
  standInCollapse.value = []
  handleDialogVisible.value = true
  handlingPoint.value = null
  if (row.disputeType === 'ITEM_NOT_FOUND' && row.itemId) {
    foundItemApi.getById(row.itemId)
      .then((res: any) => {
        const it = res.data || {}
        handlingPoint.value = {
          name: it.dropPointName || '未指定投放点',
          location: it.dropPointLocation || it.foundLocation,
          adminName: it.dropPointAdminName,
          adminPhone: it.dropPointAdminPhone
        }
      })
      .catch(() => { handlingPoint.value = null })
  }
}

const submitHandle = async () => {
  if (!handling.value) return
  if (!handleForm.value.handlerNote.trim()) {
    ElMessage.warning('请填写处理意见')
    return
  }
  submitting.value = true
  try {
    const approved = handleForm.value.approved
    await disputeApi.handle(handling.value.id, {
      approved,
      rollbackItems: approved ? handleForm.value.rollbackItems : [],
      itemDispose: handleForm.value.itemDispose,
      handlerNote: handleForm.value.handlerNote.trim()
    })
    const rolled = approved && handleForm.value.rollbackItems.length
      ? '已按勾选回滚 ' + handleForm.value.rollbackItems.length + ' 项积分'
      : (approved ? '未勾选回滚项，积分未变动' : '已驳回，积分未变动')
    ElMessage.success((approved ? '已通过：' : '已驳回：') + rolled + '；物品处置 = ' + handleForm.value.itemDispose)
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
    await cameraLogAdminApi.create({ ...form, dropPointId: form.dropPointId, applyReason: form.applyReason.trim() })
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

watch(userRoleTab, () => {
  userPage.value = 1
  loadUsers()
})

watch(activeTab, (tab) => {
  if (tab === 'credits') loadCreditBoard()
  if (tab === 'users') {
    if (!users.value.length) loadUsers()
    if (!points.value.length) loadPoints()
  }
  if (tab === 'disputes' && !adminDisputes.value.length) loadAdminDisputes()
  if (tab === 'risk' && !riskWarnings.value.length) loadRisk()
  if (tab === 'camera-logs' && !cameraLogs.value.length) { loadPoints(); loadCameraLogs() }
  if (tab === 'points' && !points.value.length) loadPoints()
  if (tab === 'reports' && !reports.value.length) loadReports()
})

onMounted(loadDashboard)
</script>

<style scoped>
.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
  margin-top: 4px;
}
.rollback-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
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

.unassigned {
  color: #909399;
  font-size: 12px;
}

.station-phone {
  color: #26745c;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.point-check-note {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
  font-size: 13px;
}
.point-check-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
.point-check-missing {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #b07b16;
  font-size: 12px;
}
.point-check-missing-wrap { display: flex; flex-direction: column; gap: 4px; }
.sla-line { font-size: 12px; color: #909399; }
.sub-meta { font-size: 11px; color: #909399; margin-top: 2px; }
.danger-text { color: #c45656; }
.standin-tag {
  display: inline-block;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 3px;
  background: #fdf6ec;
  color: #b07b16;
  font-size: 11px;
}
.standin-title { font-size: 13px; color: #909399; }
.standin-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}
.standin-hint { color: #909399; font-size: 12px; }
</style>
