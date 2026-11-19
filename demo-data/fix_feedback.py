import io, sys
p = r'D:\codex\codex-data\schoollose\frontend\src\views\FeedbackView.vue'
s = io.open(p, encoding='utf-8').read()

pairs = []

pairs.append((
    '适用于：现场找不到物品（待取件）、领取后物品不符/损坏、物品被冒领等情况',
    '适用于：领取后物品不符、领取后物品损坏、物品被冒领、其他纠纷；现场找不到实物请到「我的认领」点「找不到物品，报站点核查」'
))

pairs.append((
    '''          <el-radio value="ITEM_NOT_FOUND">现场找不到物品</el-radio>\n''',
    ''
))

pairs.append((
    '''            「找不到物品」用于<b>已通过审核、尚未取走</b>的认领：到站货架没有实物，由投放点核查货架与监控；\n            「不符 / 损坏」是取走之后才发现的问题，定责后物品转「待归还」。''',
    '''            「不符」「损坏」是<b>取走之后</b>才发现的问题：定责后物品转「待归还」，损坏件作废、不再重新公开。\n            现场找不到实物的，请回「我的认领」点「找不到物品，报站点核查」，走投放点现场核查流程。'''
))

pairs.append((
    '''        <el-form-item v-else-if="disputeForm.disputeType === 'ITEM_NOT_FOUND'" label="认领订单" required>\n          <el-select v-model="disputeForm.applyId" placeholder="选择「已通过、待取件」的认领订单" style="width: 100%">\n            <el-option v-for="a in pendingPickupClaims" :key="a.id" :value="a.id"\n              :label="`#${a.id} ${a.itemTitle || '物品' + a.itemId}（待取件）`" />\n          </el-select>\n        </el-form-item>\n''',
    ''
))

pairs.append((
    """const pendingPickupClaims = computed(() => myClaims.value.filter((a) => a.applyStatus === 4))\n""",
    ''
))

pairs.append((
    '「纠纷申诉：」在「我的纠纷申诉」tab 发起，适用于领取后物品不符、被冒领等',
    '「纠纷申诉：」在「我的纠纷申诉」tab 发起，适用于领取后物品不符、损坏、被冒领等'
))

for old, new in pairs:
    if old not in s:
        print('MISS:', old[:40].replace('\n', '\\n'))
    else:
        s = s.replace(old, new, 1)
        print('OK  :', old[:40].replace('\n', '\\n'))

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('written', len(s))
