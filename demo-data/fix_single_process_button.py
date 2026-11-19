# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\frontend\src\views\PointAdminView.vue'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new, 1)

# 1) 列表说明：以「处理」为核心
rep('''            title="先回填现场事实（实物是否在架、有无损坏、凭证是否齐全），再选结论：找不到物品可定「实物在站 / 从未投放 / 被他人拿走」，损坏可定「站点责 / 认领人责 / 发布人责」——选中后系统当场按规则表处理物品状态与积分；定不了就选「其他」转系统管理员。"''',
'''            title="每张工单只用一个「处理」按钮：由你在弹窗里决定这张单怎么结、积分怎么回滚，并填写原因。选中具体处理方式后系统当场执行（物品状态 + 积分）；定不了就选「其他」，工单转为「处理中」并交系统管理员。"''', 'tab-alert')

# 2) 状态筛选
rep('''              <el-option label="待回填（需我处理）" :value="0" />
              <el-option label="已通过（已定责）" :value="1" />
              <el-option label="已驳回" :value="2" />''',
'''              <el-option label="未处理（待我处理）" :value="0" />
              <el-option label="处理中（等管理员/等证据）" :value="3" />
              <el-option label="已处理 · 通过" :value="1" />
              <el-option label="已处理 · 驳回" :value="2" />''', 'status-filter')

# 3) 状态列改三态
rep('''            <el-table-column label="核验状态" width="190">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" size="small" type="success">已结案</el-tag>
                <el-tag v-else-if="row.pointCheckedAt" size="small" type="success">已回填</el-tag>
                <el-tag v-else size="small" :type="row.pointSlaOverdue ? 'danger' : 'warning'">
                  {{ row.pointSlaOverdue ? '超 24h 未回填' : '待核验' }}
                </el-tag>''',
'''            <el-table-column label="处理状态" width="190">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" size="small" type="success">已处理 · 通过</el-tag>
                <el-tag v-else-if="row.status === 2" size="small" type="danger">已处理 · 驳回</el-tag>
                <el-tag v-else-if="row.status === 3" size="small" type="primary">处理中</el-tag>
                <el-tag v-else size="small" :type="row.pointSlaOverdue ? 'danger' : 'warning'">
                  {{ row.pointSlaOverdue ? '未处理 · 超 24h' : '未处理' }}
                </el-tag>
                <div v-if="row.status === 3" class="sub-text">已转系统管理员定责</div>''', 'status-column')

# 4) 操作列收敛成一个「处理」按钮
rep('''            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openDisputeCheck(row)">
                  {{ row.pointCheckedAt ? '更新核验' : '现场核验' }}
                </el-button>
                <el-button size="small" @click="openCameraApply(row)">调监控</el-button>
              </template>
            </el-table-column>''',
'''            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openDisputeCheck(row)">
                  {{ row.status === 0 ? '处理' : '查看/重处理' }}
                </el-button>
              </template>
            </el-table-column>''', 'action-column')

# 5) 弹窗：一个按钮里完成「选处理方式 + 填回滚原因」
rep('''    <el-dialog v-model="disputeCheckVisible" title="现场核验并给出结论" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="先写亲眼确认的事实，再选结论。前几项由系统当场按规则表结案（物品状态与积分自动处理）；选「其他」只留痕并转系统管理员定责。"
      />''',
'''    <el-dialog v-model="disputeCheckVisible" title="处理纠纷工单" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="你在这里决定这张单怎么结、积分怎么回滚：选中具体方式后系统立即执行并通知相关人；「处理原因」必填，会写进工单记录。定不了就选「其他」，工单转为「处理中」交系统管理员。"
      />''', 'dialog-title')

rep('''      <el-input
        v-model="disputeCheckNote"
        type="textarea"
        :rows="5"
        maxlength="1000"
        show-word-limit
        placeholder="示例：15:20 到图书馆一层服务台核对，货架 A 区未见该物品；该区有监控覆盖，交付画面待调取；现场未发现其他认领记录。"
      />''',
'''      <div class="verdict-title" style="margin-bottom: 6px">处理原因（必填）</div>
      <el-input
        v-model="disputeCheckNote"
        type="textarea"
        :rows="4"
        maxlength="1000"
        show-word-limit
        placeholder="示例：15:20 到图书馆一层服务台核对，货架 A 区未见该物品，监控显示被他人取走，故认定物品已不在站。"
      />''', 'dialog-note')

rep('''        <div class="verdict-title">现场结论</div>''',
'''        <div class="verdict-title">处理方式（决定积分怎么回滚）</div>''', 'verdict-title')

rep('''      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          {{ disputeVerdict === 'OTHER' ? '提交核验并转管理员' : '提交并结案' }}
        </el-button>
      </template>''',
'''      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button @click="applyCameraFromProcess">申请调监控</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          {{ disputeVerdict === 'OTHER' ? '标记处理中并转管理员' : '确认处理并结案' }}
        </el-button>
      </template>''', 'dialog-footer')

# 6) 提交提示与「从处理弹窗跳监控」
rep('''    ElMessage.success(verdict === 'OTHER'
      ? '已回填核验事实，工单已转系统管理员定责'
      : '已按现场结论结案：物品状态与积分已按规则处理')''',
'''    ElMessage.success(verdict === 'OTHER'
      ? '已标记为「处理中」并转系统管理员定责'
      : '已处理结案：物品状态与积分已按规则处理')''', 'submit-msg')

rep('''const openDisputeCheck = (row: any) => {''',
'''// 处理弹窗里点「申请调监控」：先收起本弹窗，再打开调监控申请
const applyCameraFromProcess = () => {
  const row = currentDispute.value
  if (!row) return
  disputeCheckVisible.value = false
  openCameraApply(row)
}

const openDisputeCheck = (row: any) => {''', 'camera-bridge')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
