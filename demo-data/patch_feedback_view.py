# -*- coding: utf-8 -*-
"""把 FeedbackView 的申诉弹窗改成五类分开，并让「找不到物品」只列待取件订单。"""
import io, sys, pathlib

p = pathlib.Path(r"D:\codex\codex-data\schoollose\frontend\src\views\FeedbackView.vue")
raw = p.read_bytes()
bom = raw.startswith(b"\xef\xbb\xbf")
text = raw.decode("utf-8-sig")
crlf = "\r\n" in text
if crlf:
    text = text.replace("\r\n", "\n")

OLD_RADIO = '''        <el-form-item label="申诉类型" required>
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio value="ITEM_MISMATCH">物品不符</el-radio>
            <el-radio value="ITEM_DAMAGED">物品损坏</el-radio>
            <el-radio value="FALSE_CLAIM">物品被冒领</el-radio>
            <el-radio value="OTHER">其他纠纷</el-radio>
          </el-radio-group>
        </el-form-item>
'''

NEW_RADIO = '''        <el-form-item label="申诉类型" required>
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio value="ITEM_MISMATCH">物品不符</el-radio>
            <el-radio value="ITEM_DAMAGED">物品损坏</el-radio>
            <el-radio value="ITEM_NOT_FOUND">现场找不到物品</el-radio>
            <el-radio value="FALSE_CLAIM">物品被冒领</el-radio>
            <el-radio value="OTHER">其他纠纷</el-radio>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; line-height: 1.6; margin-top: 6px">
            「找不到物品」用于<b>已通过审核、尚未取走</b>的认领：到站货架没有实物，由投放点核查货架与监控；
            「不符 / 损坏」是取走之后才发现的问题，定责后物品转「待归还」。
          </div>
        </el-form-item>
'''

OLD_SELECT = '''        <el-form-item v-else label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择已完成领取的认领订单" style="width: 100%">
'''

NEW_SELECT = '''        <el-form-item v-else-if="disputeForm.disputeType === 'ITEM_NOT_FOUND'" label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择「已通过、待取件」的认领订单" style="width: 100%">
            <el-option v-for="a in pendingPickupClaims" :key="a.id" :value="a.id"
              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}（待取件）`" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="认领订单" required>
          <el-select v-model="disputeForm.applyId" placeholder="选择已完成领取的认领订单" style="width: 100%">
'''

OLD_DECL = '''const completedClaims = ref<ClaimApply[]>([])
'''

NEW_DECL = '''const myClaims = ref<ClaimApply[]>([])
const completedClaims = computed(() => myClaims.value.filter((a) => a.applyStatus === 1))
const pendingPickupClaims = computed(() => myClaims.value.filter((a) => a.applyStatus === 4))
'''

OLD_LOAD = '''  completedClaims.value = claimRes.data.records.filter((a) => a.applyStatus === 1)
'''

NEW_LOAD = '''  myClaims.value = claimRes.data.records
'''

OLD_IMPORT = '''import { ref, onMounted } from 'vue'
'''

NEW_IMPORT = '''import { ref, computed, onMounted } from 'vue'
'''

OLD_TIP = '''            适用于：领取后发现物品不符、物品被冒领、物品在投放点丢失等情况'''
NEW_TIP = '''            适用于：现场找不到物品（待取件）、领取后物品不符/损坏、物品被冒领等情况'''

pairs = [
    ("import", OLD_IMPORT, NEW_IMPORT),
    ("radio", OLD_RADIO, NEW_RADIO),
    ("select", OLD_SELECT, NEW_SELECT),
    ("decl", OLD_DECL, NEW_DECL),
    ("load", OLD_LOAD, NEW_LOAD),
    ("tip", OLD_TIP, NEW_TIP),
]

fails = []
for name, old, new in pairs:
    if old not in text:
        fails.append(name)
        continue
    text = text.replace(old, new, 1)

if crlf:
    text = text.replace("\n", "\r\n")
out = text.encode("utf-8")
if bom:
    out = b"\xef\xbb\xbf" + out
p.write_bytes(out)
print("crlf=", crlf, "bom=", bom, "fails=", fails)
