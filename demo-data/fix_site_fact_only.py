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

# 1) 顶部说明：站点只做核对/调监控/沟通
rep('''            title="每张工单只用一个「处理」按钮：由你在弹窗里决定这张单怎么结、积分怎么回滚，并填写原因。选中具体处理方式后系统当场执行（物品状态 + 积分）；定不了就选「其他」，工单转为「处理中」并交系统管理员。"''',
'''            title="站点只负责核对实物、调监控、联系沟通：把看到的事实写清楚提交，工单即转「处理中」交给系统管理员。积分回滚由系统管理员按你反馈的情况勾选处理，站点不预设结论、不碰积分。"''', 'tab-alert')

# 2) 操作列恢复两个按钮
rep('''            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openDisputeCheck(row)">
                  {{ row.status === 0 ? '处理' : '查看/重处理' }}
                </el-button>
              </template>
            </el-table-column>''',
'''            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openDisputeCheck(row)">
                  {{ row.pointCheckedAt ? '补充核验' : '现场核验' }}
                </el-button>
                <el-button size="small" @click="openCameraApply(row)">调监控</el-button>
              </template>
            </el-table-column>''', 'action-column')

# 3) 弹窗只留事实
rep('''    <el-dialog v-model="disputeCheckVisible" title="处理纠纷工单" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="你在这里决定这张单怎么结、积分怎么回滚：选中具体方式后系统立即执行并通知相关人；「处理原因」必填，会写进工单记录。定不了就选「其他」，工单转为「处理中」交系统管理员。"
      />''',
'''    <el-dialog v-model="disputeCheckVisible" title="现场核验（只反馈事实）" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="写清你核对到的事实即可：实物在不在架、有无损坏、凭证与监控画面看到什么、联系过谁。提交后工单转「处理中」，积分怎么回滚由系统管理员据此定责。"
      />''', 'dialog-title')

rep('''      <div class="verdict-title" style="margin-bottom: 6px">处理原因（必填）</div>
      <el-input
        v-model="disputeCheckNote"
        type="textarea"
        :rows="4"''',
'''      <div class="verdict-title" style="margin-bottom: 6px">现场情况（必填）</div>
      <el-input
        v-model="disputeCheckNote"
        type="textarea"
        :rows="6"''', 'dialog-note')

rep('''      <div class="verdict-box">
        <div class="verdict-title">处理方式（决定积分怎么回滚）</div>
        <el-radio-group v-model="disputeVerdict" class="verdict-group">
          <el-radio v-for="opt in siteVerdictOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button @click="applyCameraFromProcess">申请调监控</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          {{ disputeVerdict === 'OTHER' ? '标记处理中并转管理员' : '确认处理并结案' }}
        </el-button>
      </template>''',
'''      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          提交核验（转系统管理员）
        </el-button>
      </template>''', 'dialog-footer')

# 4) script：删结论选项，提交只传事实
rep('''const disputeVerdict = ref<string>('OTHER')
const SITE_VERDICT_OPTIONS: Record<string, { value: string; text: string }[]> = {
  ITEM_NOT_FOUND: [
    { value: 'FOUND', text: '实物在站（误报）：认领资格保留，三方积分都不动，继续取件' },
    { value: 'NOT_DELIVERED', text: '从未实际投放：招领作废，回滚发布人拾获 3 分并扣 2 分，申请驳回' },
    { value: 'STOLEN', text: '被他人拿走：物品转「丢失待查」，发布人无责，认领人保留资格' },
    { value: 'OTHER', text: '其他：现场无法判定（等监控 / 等补充证据），转系统管理员定责' }
  ],
  ITEM_DAMAGED: [
    { value: 'SITE_FAULT', text: '站点保管责任：不扣任何用户分，记站点考核' },
    { value: 'CLAIMER_FAULT', text: '认领人责任：回滚其取件 +1 并计一次误领' },
    { value: 'PUBLISHER_FAULT', text: '发布人责任：保留其奖励，另扣发布人 2 分' },
    { value: 'OTHER', text: '其他：现场定不了责，转系统管理员' }
  ]
}
const siteVerdictOptions = computed(() => SITE_VERDICT_OPTIONS[currentDispute.value?.disputeType] || [
  { value: 'OTHER', text: '该类型由系统管理员定责：本次只回填事实并转交' }
])

// 处理弹窗里点「申请调监控」：先收起本弹窗，再打开调监控申请
const applyCameraFromProcess = () => {
  const row = currentDispute.value
  if (!row) return
  disputeCheckVisible.value = false
  openCameraApply(row)
}

const openDisputeCheck = (row: any) => {
  currentDispute.value = row
  disputeCheckNote.value = row.pointCheckNote || ''
  disputeVerdict.value = 'OTHER'
  disputeCheckVisible.value = true
}''',
'''const openDisputeCheck = (row: any) => {
  currentDispute.value = row
  disputeCheckNote.value = row.pointCheckNote || ''
  disputeCheckVisible.value = true
}''', 'script-options')

rep('''  try {
    const verdict = disputeVerdict.value
    await disputeApi.pointCheck(currentDispute.value.id, disputeCheckNote.value.trim(), verdict)
    ElMessage.success(verdict === 'OTHER'
      ? '已标记为「处理中」并转系统管理员定责'
      : '已处理结案：物品状态与积分已按规则处理')''',
'''  try {
    await disputeApi.pointCheck(currentDispute.value.id, disputeCheckNote.value.trim())
    ElMessage.success('现场情况已提交，工单转为「处理中」，等系统管理员定责')''', 'script-submit')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('point admin saved')
