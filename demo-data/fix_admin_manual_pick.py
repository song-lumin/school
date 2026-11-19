# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\frontend\src\views\AdminView.vue'
s = io.open(p, encoding='utf-8').read()

def rep(old, new, tag):
    global s
    c = s.count(old)
    print(tag, 'match=', c)
    if c != 1:
        raise SystemExit('ABORT ' + tag)
    s = s.replace(old, new, 1)

# 1) 表单：结论 + 勾选回滚项 + 物品处置 + 原因
rep('''        <el-form-item label="处理结果">
          <el-radio-group v-if="handling?.disputeType === 'ITEM_NOT_FOUND'" v-model="handleForm.outcome">
            <el-radio value="FOUND">实物在站（误报，继续取件，不动积分）</el-radio>
            <el-radio value="NOT_DELIVERED">从未实际投放（招领作废 + 回滚发布人加分并扣 2 分）</el-radio>
            <el-radio value="STOLEN">被他人拿走（物品转「丢失待查」，发布人无责）</el-radio>
          </el-radio-group>
          <template v-else-if="handling?.disputeType === 'ITEM_DAMAGED'">
            <el-radio-group v-model="handleForm.outcome">
              <el-radio value="SITE_FAULT">站点保管责任（不扣任何用户分，记站点考核）</el-radio>
              <el-radio value="CLAIMER_FAULT">认领人责任（回滚其 +1、计一次误领）</el-radio>
              <el-radio value="PUBLISHER_FAULT">发布人责任（隐瞒破损，扣发布人 2 分）</el-radio>
            </el-radio-group>
            <div style="font-size: 12px; color: #909399; line-height: 1.7; margin-top: 6px">
              损坏件不论定责给谁，站点收回后都作废归档、不再回公开池；发布人的拾获与交物奖励一律不回收。
            </div>
          </template>
          <el-radio-group v-else v-model="handleForm.approved">
            <el-radio :value="true">通过（{{ approveEffectText }}）</el-radio>
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
        </el-form-item>''',
'''        <el-form-item label="处理结论">
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
        </el-form-item>''', 'form-block')

# 2) 表单状态
rep('''const handleForm = ref<{ approved: boolean; outcome: string; handlerNote: string }>({ approved: true, outcome: 'FOUND', handlerNote: '' })''',
'''const handleForm = ref<{ approved: boolean; rollbackItems: string[]; itemDispose: string; handlerNote: string }>(
  { approved: true, rollbackItems: [], itemDispose: 'NONE', handlerNote: '' }
)''', 'form-state')

rep('''  handleForm.value = {
    approved: true,
    // 损坏默认按「站点保管责任」：现实中多数损坏发生在保管/流转环节，定责人可改
    outcome: row.disputeType === 'ITEM_DAMAGED' ? 'SITE_FAULT' : 'FOUND',
    handlerNote: ''
  }''',
'''  handleForm.value = {
    approved: true,
    rollbackItems: [],
    itemDispose: 'NONE',
    handlerNote: ''
  }''', 'form-init')

# 3) 提交：改走手动回滚
rep('''  try {
    const type = handling.value.disputeType
    const isNotFound = type === 'ITEM_NOT_FOUND'
    const isDamaged = type === 'ITEM_DAMAGED'
    await disputeApi.handle(handling.value.id, {
      // 找不到物品用 outcome 三分支定结论；损坏用 outcome 定责方；其余类型尊重「通过 / 驳回」
      approved: isNotFound || isDamaged ? true : handleForm.value.approved,
      outcome: isNotFound || isDamaged ? handleForm.value.outcome : undefined,
      handlerNote: handleForm.value.handlerNote.trim()
    })
    const notFoundText: Record<string, string> = {
      FOUND: '实物在站，已通知认领人重新核对取件',
      NOT_DELIVERED: '招领已作废，发布人拾获积分已回滚并扣 2 分，其他认领申请已解锁',
      STOLEN: '物品转「丢失待查」，发布人不扣分，认领人保留取件资格'
    }
    const passText = type === 'ITEM_MISMATCH'
      ? '认领人 +1 已回滚并计一次误领，发布人奖励保留，物品转「待归还」等待站点收回'
      : type === 'ITEM_DAMAGED'
        ? (DAMAGED_FAULT_TEXT[handleForm.value.outcome] || '') + '，物品转「待归还」由站点收回作废'
        : type === 'FALSE_CLAIM'
          ? '冒领人扣 5 分、计一次误领、其 +1 已回滚，发布人奖励保留，物品转「待归还」'
          : '发布人奖励已回滚，物品已作废'
    ElMessage.success((isNotFound
      ? '已完成核查：' + (notFoundText[handleForm.value.outcome] || '')
      : (isNotFound || isDamaged ? '已定责：' + passText : (handleForm.value.approved ? '已通过：' + passText : '已驳回')))
      + '；结论与线下动作已通知站点负责人')''',
'''  try {
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
    ElMessage.success((approved ? '已通过：' : '已驳回：') + rolled + '；物品处置 = ' + handleForm.value.itemDispose)''', 'submit-handle')

# 4) 表单提示样式
rep('''<style scoped>''',
'''<style scoped>
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
}''', 'style')

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('admin saved')
