<template>
  <div class="point-admin-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>投放点管理</span>
          <el-select v-model="selectedPointId" placeholder="选择站点" style="width: 260px" @change="fetchAll">
            <el-option v-for="p in myPoints" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </template>

      <el-alert
        v-if="isSysAdmin"
        type="info"
        :closable="false"
        style="margin-bottom:10px"
        title="系统管理员视角：本页等同于站点管理员的全部权限 —— 下拉可切换任意投放点，做巡检核对、下架、确认归还入库；「纠纷核验」页签展示全部点位工单（站点负责人未在岗时可代填核验）。" />

      <el-alert
        v-if="!myPoints.length"
        type="warning"
        :closable="false"
        style="margin-bottom:10px"
        title="当前账号没有绑定任何投放点：本页只显示点位负责人自己点位上的物品与纠纷；系统管理员请到「纠纷管理」查看全部工单并定责。" />

      <el-row :gutter="12" class="stat-row">
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#909399">{{ stats.pending }}</div><div class="stat-label">待投放</div><div class="stat-sub">发布未点已投放</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#409eff">{{ stats.placed }}</div><div class="stat-label">已投放</div><div class="stat-sub">含在站+已领待补</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#e6a23c">{{ stats.picked }}</div><div class="stat-label">已领取</div><div class="stat-sub">失主已取走实物</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card stat-physical"><div class="stat-value" style="color:#67c23a">{{ stats.onSite }}</div><div class="stat-label">在站实物数</div><div class="stat-sub">= 已投放 - 已领取</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#e6a23c">{{ stats.checking }}</div><div class="stat-label">待核对</div><div class="stat-sub">hand_in_status=1</div></el-card></el-col>
        <el-col :span="4"><el-card shadow="never" class="stat-card"><div class="stat-value" style="color:#67c23a">{{ stats.checked }}</div><div class="stat-label">已核对</div><div class="stat-sub">已发/待补发积分</div></el-card></el-col>
      </el-row>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="在站管理清单" name="active">
          <div class="filter-bar">
            <el-input v-model="filter.keyword" placeholder="搜索物品名/描述/位置" style="width:240px" clearable />
            <el-select v-model="filter.category" placeholder="分类" clearable style="width:130px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="filter.biz" placeholder="业务状态" clearable style="width:170px">
              <el-option label="待投放" value="pending" />
              <el-option label="已投放" value="placed" />
              <el-option label="已领取(保障期内)" value="picked" />
              <el-option label="待归还" value="returning" />
              <el-option label="待核对" value="checking" />
              <el-option label="已核对" value="checked" />
            </el-select>
            <el-button @click="resetFilter">重置</el-button>
          </div>
          <el-alert type="info" :closable="false" style="margin:12px 0"
            title="巡检要点：对照本清单清点实物。若同一实物对应多条招领帖（重复/虚假投放），选中可疑帖点'管理员下架'并填写原因；失主已先领取的，查看领取凭证后点'核对补发'。" />
          <el-table :data="activeList" v-loading="loading" stripe>
            <el-table-column prop="item.id" label="ID" width="70" />
            <el-table-column label="图片" width="80">
              <template #default="{ row }">
                <el-image
                  v-if="row.item.images && row.item.images.length && !isPlaceholderImage(row.item.images[0])"
                  :src="row.item.images[0]"
                  :preview-src-list="row.item.images"
                  fit="cover"
                  style="width:48px;height:48px;border-radius:4px"
                  preview-teleported
                >
                  <template #error><div class="thumb-fallback"><el-icon><Box /></el-icon></div></template>
                </el-image>
                <div v-else class="thumb-fallback"><el-icon><Box /></el-icon></div>
              </template>
            </el-table-column>
            <el-table-column label="物品" min-width="200">
              <template #default="{ row }">
                <router-link :to="`/items/${row.item.id}`" class="item-link">{{ row.item.title }}</router-link>
                <div class="sub-text">{{ row.item.foundLocation }} · {{ formatTime(row.item.publishedAt) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="100"><template #default="{ row }"><el-tag size="small">{{ row.item.category || '-' }}</el-tag></template></el-table-column>
            <el-table-column label="业务状态" width="130"><template #default="{ row }"><el-tag size="small" :type="bizTag(row).type">{{ bizTag(row).text }}</el-tag></template></el-table-column>
            <el-table-column label="核对状态" width="130"><template #default="{ row }"><el-tag size="small" :type="checkTag(row).type">{{ checkTag(row).text }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.item.itemStatus === 7" type="success" size="small" :loading="returnInLoading" @click="handleReturnIn(row)">确认归还入库</el-button>
                <el-button v-if="row.handInStatus === 1" type="primary" size="small" @click="openCheckDialog(row)">{{ row.item.itemStatus === 3 ? '核对补发' : '核对发分' }}</el-button>
                <el-button v-if="row.item.itemStatus !== 3 && row.item.itemStatus !== 5 && row.item.itemStatus !== 0" type="danger" size="small" plain @click="openTakedownDialog(row)">管理员下架</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="在站暂无物品" /></template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="disputeTabLabel" name="dispute">          <el-alert
            type="warning"
            :closable="false"
            style="margin: 12px 0"
            title="站点负责核实物、调监控、联系双方并给出结论；系统管理员只按你的结论执行积分，不重判事实。核不清时只提交事实、转系统管理员。"
          />
          <div class="filter-bar" style="margin-top: 4px">
            <el-select v-model="disputeFilter.type" placeholder="纠纷类型" clearable style="width: 160px" @change="reloadDisputes">
              <el-option label="物品不符" value="ITEM_MISMATCH" />
              <el-option label="物品损坏" value="ITEM_DAMAGED" />
              <el-option label="被冒领" value="FALSE_CLAIM" />
              <el-option label="找不到物品" value="ITEM_NOT_FOUND" />
              <el-option label="其他" value="OTHER" />
            </el-select>
            <el-select v-model="disputeFilter.status" placeholder="全部状态" clearable style="width: 180px" @change="reloadDisputes">
              <el-option label="未处理（待我处理）" :value="0" />
              <el-option label="处理中（等补充核对）" :value="3" />
              <el-option label="待积分处置（已提交结论）" :value="4" />
              <el-option label="已结案 · 通过" :value="1" />
              <el-option label="已结案 · 驳回" :value="2" />
            </el-select>
            <el-button @click="reloadDisputes">刷新</el-button>
            <span class="sub-text" style="margin:0">共 {{ disputeTotal }} 条</span>
          </div>
          <el-table :data="pointDisputes" v-loading="disputeLoading" stripe>
            <el-table-column prop="id" label="工单" width="70" />
            <el-table-column label="类型" width="150">
              <template #default="{ row }">
                <el-tag size="small" :type="DISPUTE_TYPE_MAP[row.disputeType]?.type || 'info'">
                  {{ DISPUTE_TYPE_MAP[row.disputeType]?.text || row.disputeType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="物品" min-width="160">
              <template #default="{ row }">
                <router-link v-if="row.itemId" :to="`/items/${row.itemId}`" class="item-link">{{ row.itemTitle || ('#' + row.itemId) }}</router-link>
                <span v-else class="sub-text">未关联</span>
                <div class="sub-text">{{ row.dropPointName || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="申诉人" width="150">
              <template #default="{ row }">
                {{ row.applicantName || '-' }}
                <div class="sub-text">{{ row.applicantPhone || '未留电话' }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="申诉说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="证据" width="80">
              <template #default="{ row }">
                <el-image
                  v-if="row.evidenceImages && row.evidenceImages.length"
                  :src="row.evidenceImages[0]"
                  :preview-src-list="row.evidenceImages"
                  fit="cover"
                  style="width: 40px; height: 40px; border-radius: 3px"
                  preview-teleported
                >
                  <template #error><div class="thumb-fallback"><el-icon><Box /></el-icon></div></template>
                </el-image>
                <span v-else class="sub-text">无</span>
              </template>
            </el-table-column>
            <el-table-column label="处理状态" width="190">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" size="small" type="success">已结案 · 通过</el-tag>
                <el-tag v-else-if="row.status === 2" size="small" type="danger">已结案 · 驳回</el-tag>
                <el-tag v-else-if="row.status === 4" size="small" type="warning">待积分处置</el-tag>
                <el-tag v-else-if="row.status === 3" size="small" type="primary">处理中</el-tag>
                <el-tag v-else size="small" :type="row.pointSlaOverdue ? 'danger' : 'warning'">
                  {{ row.pointSlaOverdue ? '未处理 · 超 24h' : '未处理' }}
                </el-tag>
                <div v-if="row.status === 3" class="sub-text">等补充核对或系统管理员定责</div>
                <div v-if="row.status === 4" class="sub-text">
                  结论：{{ row.siteVerdictText || row.siteVerdict }} · 等系统管理员处置积分
                </div>
                <div v-if="row.status === 4 && row.needReview === 1" class="sub-text warn-text">
                  涉及站点责任/不可抗力，已强制转系统复核
                </div>
                <div v-if="!row.pointCheckedAt" class="sub-text">已等待 {{ row.pendingHours ?? 0 }} 小时</div>
                <div v-if="!row.pointCheckedAt && row.pointRemindCount > 0" class="sub-text warn-text">
                  已被催办 {{ row.pointRemindCount }} 次，请尽快回填
                </div>
                <div v-if="row.siteUnresponsive === 1" class="sub-text danger-text">
                  主管已标记本单「站点未响应」
                </div>
                <div v-else-if="row.pointCheckStandIn" class="sub-text warn-text">系统已代填（你未在岗时核实）</div>
                <div v-if="row.pointCheckNote" class="check-note">{{ row.pointCheckNote }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openDisputeCheck(row)">
                  {{ row.pointCheckedAt ? '补充核验' : '现场核验' }}
                </el-button>
                <el-button size="small" @click="openCameraApply(row)">调监控</el-button>
                <el-button size="small" type="warning" plain @click="openChat(row)">沟通</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty v-if="isSysAdmin" description="全部点位当前没有符合条件的纠纷工单，可清空筛选或点「刷新」再试" />
              <el-empty v-else-if="!myPoints.length" description="当前账号未绑定投放点，看不到任何点位工单">
                <div class="sub-text" style="line-height:1.8;max-width:520px">
                  站点核验请用点位负责人账号登录：pointadmin1（周敏）负责图书馆 / 宿舍区 / 体育馆，pointadmin2（吴涛）负责学生活动中心 / 教学楼。<br>
                  系统管理员请到「纠纷管理」查看全部工单并定责。
                </div>
              </el-empty>
              <el-empty v-else description="当前点位没有符合条件的纠纷工单，可清空筛选或点「刷新」再试" />
            </template>
          </el-table>
          <el-pagination
            v-if="disputeTotal > disputePageSize"
            layout="prev, pager, next"
            :total="disputeTotal"
            :page-size="disputePageSize"
            :current-page="disputePage"
            style="margin-top: 10px; justify-content: flex-end; display: flex"
            @current-change="onDisputePageChange"
          />

          <el-divider content-position="left">我提交的监控调取申请</el-divider>
          <el-alert
            type="info"
            :closable="false"
            style="margin-bottom: 10px"
            title="申请由系统管理员审批留痕。批准后你按审批结论到现场调取画面，把看到的事实通过「现场核验」回填；能据此下结论的当场选结论结案，仍定不了的选「其他」转系统管理员。"
          />
          <el-table :data="cameraLogs" v-loading="cameraLoading" size="small" stripe>
            <el-table-column prop="id" label="申请" width="70" />
            <el-table-column prop="dropPointName" label="投放点" width="150" />
            <el-table-column label="关联物品" min-width="150">
              <template #default="{ row }">
                <span v-if="row.itemTitle">{{ row.itemTitle }}</span>
                <span v-else class="sub-text">未关联</span>
              </template>
            </el-table-column>
            <el-table-column label="调取时段" width="240">
              <template #default="{ row }">
                {{ formatTime(row.timeRangeStart) }} ~ {{ formatTime(row.timeRangeEnd) }}
              </template>
            </el-table-column>
            <el-table-column label="审批状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 0 ? '待审批' : row.status === 1 ? '已批准' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="处理说明" min-width="220">
              <template #default="{ row }">
                <div v-if="row.resultNote">{{ row.resultNote }}</div>
                <div v-else class="sub-text">等待系统管理员处理（提交后自动通知）</div>
                <div v-if="row.handledAt" class="sub-text">{{ row.handlerName || '管理员' }} · {{ formatTime(row.handledAt) }}</div>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无调取申请，在纠纷行点「调监控」即可提交" /></template>
          </el-table>
          <el-pagination
            v-if="cameraTotal > cameraPageSize"
            layout="prev, pager, next"
            :total="cameraTotal"
            :page-size="cameraPageSize"
            :current-page="cameraPage"
            style="margin-top: 10px; justify-content: flex-end; display: flex"
            @current-change="onCameraPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="disputeCheckVisible" title="现场核对与结论（站点结案）" width="700px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 10px"
        title="站点负责核实物、调监控、联系双方并给出结论。提交结论后工单转「待积分处置」，系统管理员只按结论执行积分，不重判事实。"
      />
      <p class="check-tip">
        工单 #{{ currentDispute?.id }} ·
        {{ DISPUTE_TYPE_MAP[currentDispute?.disputeType]?.text || currentDispute?.disputeType }} ·
        {{ currentDispute?.itemTitle || '物品' }}
        <span v-if="currentDispute?.dropPointName"> · {{ currentDispute.dropPointName }}</span>
      </p>
      <div class="verdict-title" style="margin-bottom: 6px">现场核对结论</div>
      <el-radio-group v-model="disputeVerdict" style="display:flex;flex-direction:column;gap:8px;margin-bottom:10px">
        <el-radio value="" border>暂不定论 · 只提交事实，转系统管理员定责</el-radio>
        <el-radio
          v-for="opt in verdictOptions"
          :key="opt.value"
          :value="opt.value"
          :label="opt.label"
          border
        />
      </el-radio-group>
      <el-alert
        v-if="selectedVerdict"
        :type="selectedVerdict.needReview ? 'warning' : 'success'"
        :closable="false"
        style="margin-bottom: 10px"
      >
        <div>物品去向：{{ selectedVerdict.dispose === 'NONE' && String(selectedVerdict.value).indexOf('DAMAGE') === 0 ? '不收回（物品留在认领人，认领完成、不再重新公开）' : (DISPOSE_LABELS[selectedVerdict.dispose] || selectedVerdict.dispose) }}</div>
        <div>应回滚积分：{{ selectedVerdictItemsText }}</div>
        <div v-if="!selectedVerdict.items.length && selectedVerdict.value !== 'REJECT'">
          该结论不撤回任何已发放积分，只处理物品与责任认定
        </div>
        <div v-if="selectedVerdict.extra">附加处罚：{{ selectedVerdict.extra }}</div>
        <div v-if="selectedVerdict.value === 'REJECT'">申诉不成立：物品状态与积分均不变，本单直接结案</div>
        <div v-if="selectedVerdict.needReview">涉及站点自身责任或不可抗力，提交后强制转系统复核</div>
      </el-alert>
      <div class="verdict-title" style="margin-bottom: 6px">现场情况（必填，至少 20 字）</div>
      <el-input
        v-model="disputeCheckNote"
        type="textarea"
        :rows="5"
        maxlength="1000"
        show-word-limit
        placeholder="写清时间、地点、看到了什么、联系过谁。示例：15:20 到图书馆一层服务台核对，货架 A 区未见该物品；监控 15:05 显示一名男生取走，已电话联系其本人，确认是拿错并愿意送回。"
      />
      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          {{ disputeVerdict ? '提交结论 · 转积分处置' : '仅提交事实 · 转系统管理员' }}
        </el-button>
      </template>
    </el-dialog>

    <ChatDialog v-model:visible="chatVisible" :dispute-id="chatDisputeId" title="沟通 · 申诉人与系统管理员" />

    <el-dialog v-model="cameraDialogVisible" title="申请调取监控" width="540px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="提交后由系统管理员审批留痕；你在站点现场按审批结果调取画面，再把看到的事实通过「现场核验」回填给系统管理员定责。"
      />
      <el-form label-width="90px">
        <el-form-item label="投放点">{{ currentDispute?.dropPointName || '当前点位' }}</el-form-item>
        <el-form-item label="关联物品">#{{ cameraForm.itemId }} {{ currentDispute?.itemTitle || '' }}</el-form-item>
        <el-form-item label="调取原因">
          <el-input
            v-model="cameraForm.applyReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：工单#12 认领人反映取件时物品不在架，需核对 15:00-16:00 东侧货架监控"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="cameraForm.timeRangeStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="cameraForm.timeRangeEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cameraDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="cameraSubmitting" @click="submitCameraApply">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkDialogVisible" title="巡检核对" width="440px">
      <p class="check-tip">
        <template v-if="currentRow?.item.itemStatus === 3">物品「{{ currentRow?.item.title }}」失主已在巡检前领取。请查看签字认领书与失物合影，确认凭证无误后补发 <b>1 积分</b>。</template>
        <template v-else>确认物品「{{ currentRow?.item.title }}」已实物在站且信息相符？核对通过后给发布人发 <b>1 积分</b>。</template>
      </p>
      <el-input v-model="checkNote" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="核对备注（可选）" />
      <template #footer>
        <el-button @click="checkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkSubmitting" @click="handleCheck">确认核对</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="takedownDialogVisible" title="管理员下架" width="480px">
      <el-alert type="warning" :closable="false" style="margin-bottom:12px" title="仅当发现重复投放、虚假招领、实物不在站时使用。下架必须填原因，原因与你的账号会写入留档。" />
      <p>下架物品：<b>{{ currentRow?.item.title }}</b>（#{{ currentRow?.item.id }}）</p>
      <el-input v-model="takedownReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请填写下架原因（必填）" />
      <template #footer>
        <el-button @click="takedownDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="takedownSubmitting" @click="handleTakedown">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box } from '@element-plus/icons-vue'
import { dropPointApi, moderationApi, archiveApi, disputeApi, cameraLogAdminApi } from '@/api'
import { DISPUTE_TYPE_MAP, DISPUTE_VERDICT_OPTIONS, CREDIT_ITEM_LABELS, DISPOSE_LABELS } from '@/types'
import type { DropPoint } from '@/types'
import { useUserStore } from '@/stores/user'
import ChatDialog from '@/components/ChatDialog.vue'

const isPlaceholderImage = (src: string) => !src || src.includes('example.com')

interface InventoryRow {
  item: any
  handInStatus: number
  handInAt?: string
  checkedAt?: string
  checkNote?: string
  creditIssued?: number
  founderName?: string
  claimerName?: string
  pickupTime?: string
}

const loading = ref(false)
const archiveLoading = ref(false)
const inventory = ref<InventoryRow[]>([])
const archiveList = ref<any[]>([])
const userStore = useUserStore()
const isSysAdmin = computed(() => userStore.isAdmin)
const myPoints = ref<DropPoint[]>([])
const selectedPointId = ref<number | null>(null)
const activeTab = ref<'active' | 'dispute' | 'archive'>('active')

const filter = ref({ keyword: '', category: '', biz: '' })

const checkDialogVisible = ref(false)
const checkSubmitting = ref(false)
const checkNote = ref('')
const currentRow = ref<InventoryRow | null>(null)
const takedownDialogVisible = ref(false)
const takedownSubmitting = ref(false)
const takedownReason = ref('')
const returnInLoading = ref(false)

// ===== 纠纷核验（回填事实 + 可选结论：前三项当场结案，OTHER 转系统管理员） =====
const pointDisputes = ref<any[]>([])
const disputeLoading = ref(false)
const disputeCheckVisible = ref(false)
const disputeSubmitting = ref(false)
const disputeCheckNote = ref('')
const disputeVerdict = ref('')
const currentDispute = ref<any>(null)
const disputeFilter = ref<{ type?: string; status?: number }>({ status: 0 })
const disputePage = ref(1)
const disputePageSize = ref(10)
const disputeTotal = ref(0)
const pendingDisputeCount = ref(0)

const fetchPointDisputes = async () => {
  disputeLoading.value = true
  try {
    const res = await disputeApi.listForPoint({
      page: disputePage.value,
      size: disputePageSize.value,
      ...(disputeFilter.value.status !== undefined && disputeFilter.value.status !== null
        ? { status: disputeFilter.value.status } : {}),
      ...(disputeFilter.value.type ? { disputeType: disputeFilter.value.type } : {})
    } as any)
    pointDisputes.value = res.data.records || []
    disputeTotal.value = Number(res.data.total || 0)
    if (disputeFilter.value.status === 0) pendingDisputeCount.value = disputeTotal.value
  } catch (e) {
    console.error('加载纠纷工单失败:', e)
  } finally { disputeLoading.value = false }
}

const reloadDisputes = () => { disputePage.value = 1; fetchPointDisputes() }
const onDisputePageChange = (p: number) => { disputePage.value = p; fetchPointDisputes() }

// 我提交的监控调取申请（审批归系统管理员，这里只看进度）
const cameraLogs = ref<any[]>([])
const cameraLoading = ref(false)
const cameraPage = ref(1)
const cameraPageSize = ref(5)
const cameraTotal = ref(0)

const fetchCameraLogs = async () => {
  cameraLoading.value = true
  try {
    const res = await cameraLogAdminApi.mine({ page: cameraPage.value, size: cameraPageSize.value })
    cameraLogs.value = res.data.records || []
    cameraTotal.value = Number(res.data.total || 0)
  } catch (e) {
    console.error('加载调取申请失败:', e)
  } finally { cameraLoading.value = false }
}

const onCameraPageChange = (p: number) => { cameraPage.value = p; fetchCameraLogs() }

// 现场结论：选了前三项等于站点当场结案，系统按规则表处理状态与积分；OTHER 只回填并转管理员
const chatVisible = ref(false)
const chatDisputeId = ref<number | null>(null)
const openChat = (row: any) => {
  chatDisputeId.value = row.id
  chatVisible.value = true
}

const openDisputeCheck = (row: any) => {
  currentDispute.value = row
  disputeCheckNote.value = row.pointCheckNote || ''
  disputeVerdict.value = ''
  disputeCheckVisible.value = true
}

/** 当前工单类型下允许的结论（后端口径保持一致，选错会被拦） */
const verdictOptions = computed(() => {
  const t = currentDispute.value?.disputeType
  return (t && (DISPUTE_VERDICT_OPTIONS as any)[t]) || []
})
const selectedVerdict = computed(() =>
  verdictOptions.value.find((o: any) => o.value === disputeVerdict.value) || null
)
const selectedVerdictItemsText = computed(() => {
  const opt = selectedVerdict.value
  if (!opt || !opt.items.length) return '无需回滚'
  return opt.items.map((i: string) => (CREDIT_ITEM_LABELS as any)[i] || i).join('、')
})

const submitDisputeCheck = async () => {
  if (!currentDispute.value) return
  const note = disputeCheckNote.value.trim()
  if (!note) { ElMessage.warning('请填写现场核对情况'); return }
  if (note.length < 20) { ElMessage.warning('请把时间、地点、看到什么写清，至少 20 字'); return }
  const verdict = disputeVerdict.value || undefined
  disputeSubmitting.value = true
  try {
    await disputeApi.pointCheck(currentDispute.value.id, note, verdict)
    ElMessage.success(verdict
      ? '结论已提交，工单转为「待积分处置」，系统管理员将据此处理积分'
      : '现场情况已提交，工单转为「处理中」，等系统管理员定责')
    disputeCheckVisible.value = false
    fetchPointDisputes()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally { disputeSubmitting.value = false }
}

const onTabChange = (tab: string | number) => {
  if (tab === 'dispute') { fetchPointDisputes(); fetchCameraLogs() }
}

/** 待我回填的工单数：站点不在岗时用它提醒自己一登录就先处理 */
const disputeTabLabel = computed(() =>
  pendingDisputeCount.value ? `纠纷核验 (${pendingDisputeCount.value})` : '纠纷核验'
)

// 站点为本点位发起监控调取申请（审批归系统管理员）
const cameraDialogVisible = ref(false)
const cameraSubmitting = ref(false)
const cameraForm = ref({ itemId: undefined as number | undefined, applyReason: '', timeRangeStart: '', timeRangeEnd: '' })

const openCameraApply = (row: any) => {
  currentDispute.value = row
  cameraForm.value = {
    itemId: row.itemId || undefined,
    applyReason: `工单#${row.id}：${DISPUTE_TYPE_MAP[row.disputeType]?.text || row.disputeType}，需核对现场监控`,
    timeRangeStart: '',
    timeRangeEnd: ''
  }
  cameraDialogVisible.value = true
}

const submitCameraApply = async () => {
  const f = cameraForm.value
  const dropPointId = currentDispute.value?.dropPointId || selectedPointId.value
  if (!dropPointId) { ElMessage.warning('未识别投放点'); return }
  if (!f.applyReason.trim() || !f.timeRangeStart || !f.timeRangeEnd) {
    ElMessage.warning('请填写调取原因和完整时间范围'); return
  }
  cameraSubmitting.value = true
  try {
    await cameraLogAdminApi.create({
      dropPointId: Number(dropPointId),
      itemId: f.itemId ? Number(f.itemId) : undefined,
      applyReason: f.applyReason.trim(),
      timeRangeStart: f.timeRangeStart,
      timeRangeEnd: f.timeRangeEnd
    })
    ElMessage.success('调取申请已提交，等待系统管理员审批')
    cameraDialogVisible.value = false
    cameraPage.value = 1
    fetchCameraLogs()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败，请确认该点位已配置监控')
  } finally { cameraSubmitting.value = false }
}

const formatTime = (t: string) => t ? t.replace('T', ' ').slice(0, 16) : '-'

const categories = ['电子产品', '证件卡类', '钥匙', '书籍资料', '衣物', '饰品', '生活用品', '其他']

const WARRANTY_DAYS = 7
const withinWarranty = (row: InventoryRow): boolean => {
  if (!row.pickupTime) return true
  const d = new Date(row.pickupTime)
  if (isNaN(d.getTime())) return true
  const deadline = new Date(d.getTime() + WARRANTY_DAYS * 86400000)
  return deadline > new Date()
}
const isArchived = (row: InventoryRow) => {
  const s = row.item.itemStatus
  if (s === 0 || s === 5) return true
  if (s === 3) {
    // 已领取：已核对完结 或 过了7天保障期 → 入留档
    if (row.handInStatus === 2) return true
    if (!withinWarranty(row)) return true
  }
  return false
}

const stats = computed(() => {
  const s = { pending: 0, placed: 0, picked: 0, onSite: 0, checking: 0, checked: 0 }
  inventory.value.forEach(r => {
    if (isArchived(r)) return
    const st = r.item.itemStatus
    if (st === 6) s.pending++
    if (st === 1 || st === 2) s.placed++
    if (st === 3 && withinWarranty(r)) s.picked++
    if (st === 1 || st === 2) s.onSite++
    if (r.handInStatus === 1) s.checking++
    if (r.handInStatus === 2) s.checked++
  })
  return s
})

const activeList = computed(() => {
  return inventory.value.filter(r => {
    if (isArchived(r)) return false
    if (filter.value.keyword) {
      const k = filter.value.keyword.toLowerCase()
      const hay = `${r.item.title} ${r.item.description || ''} ${r.item.foundLocation || ''}`.toLowerCase()
      if (!hay.includes(k)) return false
    }
    if (filter.value.category && r.item.category !== filter.value.category) return false
    if (filter.value.biz) {
      const st = r.item.itemStatus
      if (filter.value.biz === 'pending' && st !== 6) return false
      if (filter.value.biz === 'placed' && !(st === 1 || st === 2)) return false
      if (filter.value.biz === 'picked' && !(st === 3 && withinWarranty(r))) return false
      if (filter.value.biz === 'returning' && st !== 7) return false
      if (filter.value.biz === 'checking' && r.handInStatus !== 1) return false
      if (filter.value.biz === 'checked' && r.handInStatus !== 2) return false
    }
    return true
  })
})


const bizTag = (row: InventoryRow) => {
  const st = row.item.itemStatus
  if (st === 6) return { type: 'info', text: '待投放' }
  if (st === 2) return { type: 'warning', text: '认领中' }
  if (st === 1) return { type: 'success', text: '已投放' }
  if (st === 7) return { type: 'danger', text: '待归还' }
  if (st === 3) return withinWarranty(row)
    ? { type: 'primary', text: '已领取(保障期内)' }
    : { type: 'info', text: '已领取(过保)' }
  return { type: 'info', text: '未知' }
}
const checkTag = (row: InventoryRow) => {
  if (row.handInStatus === 2) return { type: 'success', text: '已核对' }
  if (row.handInStatus === 1) return { type: 'warning', text: '待核对' }
  return { type: 'info', text: '未投放' }
}

const fetchMyPoints = async () => {
  const res = await dropPointApi.list()
  const all = res.data.filter(p => p.status === 1)
  myPoints.value = all
  if (myPoints.value.length && !selectedPointId.value) selectedPointId.value = myPoints.value[0].id
}

const fetchInventory = async () => {
  if (!selectedPointId.value) return
  loading.value = true
  try {
    const res = await dropPointApi.getInventory(selectedPointId.value)
    inventory.value = res.data as InventoryRow[]
  } finally { loading.value = false }
}

const fetchArchive = async () => {
  if (!selectedPointId.value) return
  archiveLoading.value = true
  try {
    const res = await archiveApi.listItems({ dropPointId: selectedPointId.value })
    archiveList.value = res.data
  } finally { archiveLoading.value = false }
}

const fetchAll = () => { fetchInventory(); fetchArchive() }

const openCheckDialog = (row: InventoryRow) => { currentRow.value = row; checkNote.value = ''; checkDialogVisible.value = true }
const handleCheck = async () => {
  if (!selectedPointId.value || !currentRow.value) return
  checkSubmitting.value = true
  try {
    await dropPointApi.checkItem(selectedPointId.value, { itemId: currentRow.value.item.id, checkNote: checkNote.value || undefined })
    ElMessage.success('巡检完成，已发放 1 积分')
    checkDialogVisible.value = false
    fetchAll()
  } finally { checkSubmitting.value = false }
}

const openTakedownDialog = (row: InventoryRow) => { currentRow.value = row; takedownReason.value = ''; takedownDialogVisible.value = true }
const handleTakedown = async () => {
  if (!takedownReason.value.trim()) { ElMessage.warning('请填写下架原因'); return }
  if (!currentRow.value) return
  takedownSubmitting.value = true
  try {
    await moderationApi.takedownItem(currentRow.value.item.id, takedownReason.value.trim())
    ElMessage.success('已下架，记录已入留档区')
    takedownDialogVisible.value = false
    fetchAll()
  } finally { takedownSubmitting.value = false }
}

const resetFilter = () => { filter.value = { keyword: '', category: '', biz: '' } }

const handleReturnIn = async (row: InventoryRow) => {
  if (!selectedPointId.value) return
  try {
    await ElMessageBox.confirm(
      `确认「${row.item.title}」实物已收回站点？普通误领件收回后会重新公开待认领；历史遗留的损坏待归还件收回后直接作废归档，不会再进入公开池。`,
      '确认归还入库',
      { confirmButtonText: '已收回，入库', cancelButtonText: '再等等', type: 'warning' }
    )
  } catch { return }
  returnInLoading.value = true
  try {
    const res = await dropPointApi.returnItemIn(selectedPointId.value, row.item.id)
    ElMessage.success(res.data === 'VOIDED'
      ? '已收回并作废归档（物品损坏，不再重新公开）'
      : '已确认归还入库，物品重新公开待认领')
    fetchAll()
  } finally { returnInLoading.value = false }
}

watch(selectedPointId, () => fetchAll())
onMounted(async () => { await fetchMyPoints(); fetchAll(); fetchPointDisputes(); fetchCameraLogs() })
</script>

<style scoped>
.point-admin-container { max-width: 1400px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: bold; }
.stat-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: #333; margin-top: 2px; }
.stat-sub { font-size: 11px; color: #999; margin-top: 2px; }
.stat-physical { background: #f0f9eb; }
.filter-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.item-link { color: #26745c; font-weight: 500; }
.sub-text { font-size: 12px; color: #999; margin-top: 2px; }
.thumb-fallback {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  color: #b9c2bd;
  background: #f2f6f3;
  border-radius: 4px;
}
.thumb-fallback .el-icon { font-size: 20px; }
.check-tip { margin: 0 0 12px; line-height: 1.6; }
.verdict-box { margin-top: 14px; border-top: 1px dashed #dcdfe6; padding-top: 12px; }
.verdict-title { font-size: 13px; font-weight: 600; color: #4d5b54; margin-bottom: 8px; }
.verdict-group { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.verdict-group :deep(.el-radio) { height: auto; margin-right: 0; white-space: normal; line-height: 1.5; }
.check-note { font-size: 12px; color: #6b7c73; line-height: 1.5; margin-top: 4px; }
.warn-text { color: #a0601f; }
.danger-text { color: #c45656; }
</style>
