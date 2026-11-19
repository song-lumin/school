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

# 1) 弹窗：加结论单选
rep('''    <el-dialog v-model="disputeCheckVisible" title="回填现场核验" width="560px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="只写你亲眼确认的事实：实物是否在架、损坏部位、交付凭证是否齐全、监控是否可用。不要写「谁该负责」这类结论。"
      />''',
'''    <el-dialog v-model="disputeCheckVisible" title="现场核验并给出结论" width="600px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
        title="先写亲眼确认的事实，再选结论。前几项由系统当场按规则表结案（物品状态与积分自动处理）；选「其他」只留痕并转系统管理员定责。"
      />''', 'dialog-head')

rep('''      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">提交核验</el-button>
      </template>
    </el-dialog>''',
'''      <div class="verdict-box">
        <div class="verdict-title">现场结论</div>
        <el-radio-group v-model="disputeVerdict" class="verdict-group">
          <el-radio v-for="opt in siteVerdictOptions" :key="opt.value" :value="opt.value">{{ opt.text }}</el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="disputeCheckVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeSubmitting" @click="submitDisputeCheck">
          {{ disputeVerdict === 'OTHER' ? '提交核验并转管理员' : '提交并结案' }}
        </el-button>
      </template>
    </el-dialog>''', 'dialog-verdict')

# 2) script：结论状态与选项
rep('''const openDisputeCheck = (row: any) => {
  currentDispute.value = row
  disputeCheckNote.value = row.pointCheckNote || ''
  disputeCheckVisible.value = true
}''',
'''// 现场结论：选了前三项等于站点当场结案，系统按规则表处理状态与积分；OTHER 只回填并转管理员
const disputeVerdict = ref<string>('OTHER')
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

const openDisputeCheck = (row: any) => {
  currentDispute.value = row
  disputeCheckNote.value = row.pointCheckNote || ''
  disputeVerdict.value = 'OTHER'
  disputeCheckVisible.value = true
}''', 'script-options')

rep('''    await disputeApi.pointCheck(currentDispute.value.id, disputeCheckNote.value.trim())
    ElMessage.success('已回填现场核验，系统管理员将据此定责')''',
'''    const verdict = disputeVerdict.value
    await disputeApi.pointCheck(currentDispute.value.id, disputeCheckNote.value.trim(), verdict)
    ElMessage.success(verdict === 'OTHER'
      ? '已回填核验事实，工单已转系统管理员定责'
      : '已按现场结论结案：物品状态与积分已按规则处理')''', 'submit')

# 3) 标签状态：结案后显示结论
rep('''                <el-tag v-if="row.pointCheckedAt" size="small" type="success">已回填</el-tag>''',
'''                <el-tag v-if="row.status === 1" size="small" type="success">已结案</el-tag>
                <el-tag v-else-if="row.pointCheckedAt" size="small" type="success">已回填</el-tag>''', 'tag')

# 4) 样式
rep('''.check-tip { margin: 0 0 12px; line-height: 1.6; }''',
'''.check-tip { margin: 0 0 12px; line-height: 1.6; }
.verdict-box { margin-top: 14px; border-top: 1px dashed #dcdfe6; padding-top: 12px; }
.verdict-title { font-size: 13px; font-weight: 600; color: #4d5b54; margin-bottom: 8px; }
.verdict-group { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.verdict-group :deep(.el-radio) { height: auto; margin-right: 0; white-space: normal; line-height: 1.5; }''', 'style')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('saved')
