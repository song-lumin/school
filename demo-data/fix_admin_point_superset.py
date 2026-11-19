# -*- coding: utf-8 -*-
import io

def patch(path, pairs, tag):
    s = io.open(path, encoding='utf-8').read()
    ok = True
    for old, new, name in pairs:
        c = s.count(old)
        print(f'{tag}.{name}: match={c}')
        if c != 1:
            ok = False
            continue
        s = s.replace(old, new, 1)
    if ok:
        io.open(path, 'w', encoding='utf-8', newline='').write(s)
    return ok

# ---------- 后端：系统管理员看全部点位工单 ----------
be = r'D:\codex\codex-data\schoollose\backend\src\main\java\com\school\lostfound\service\impl\DisputeServiceImpl.java'
old = '''        List<DropPoint> myPoints = dropPointMapper.selectList(new LambdaQueryWrapper<DropPoint>()
                .eq(DropPoint::getAdminId, pointAdminId));
        if (myPoints.isEmpty()) {
            return emptyPage;
        }
        List<Long> pointIds = myPoints.stream().map(DropPoint::getId).toList();
        List<FoundItem> myItems = foundItemMapper.selectList(new LambdaQueryWrapper<FoundItem>()
                .in(FoundItem::getDropPointId, pointIds));
        if (myItems.isEmpty()) {
            return emptyPage;
        }
        List<Long> itemIds = myItems.stream().map(FoundItem::getId).toList();

        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Dispute::getItemId, itemIds);'''
new = '''        // 系统管理员是站点管理员的超集：不按「自己负责的点位」过滤，直接看到全部点位工单；
        // 点位管理员仍只看自己负责的点位。站点是兼职岗，管理员代看、代核验时需要这个视角。
        User operator = userMapper.selectById(pointAdminId);
        boolean isSysAdmin = operator != null
                && operator.getRole() == com.school.lostfound.enums.UserRole.SYS_ADMIN;
        List<Long> itemIds = null;
        if (!isSysAdmin) {
            List<DropPoint> myPoints = dropPointMapper.selectList(new LambdaQueryWrapper<DropPoint>()
                    .eq(DropPoint::getAdminId, pointAdminId));
            if (myPoints.isEmpty()) {
                return emptyPage;
            }
            List<Long> pointIds = myPoints.stream().map(DropPoint::getId).toList();
            List<FoundItem> myItems = foundItemMapper.selectList(new LambdaQueryWrapper<FoundItem>()
                    .in(FoundItem::getDropPointId, pointIds));
            if (myItems.isEmpty()) {
                return emptyPage;
            }
            itemIds = myItems.stream().map(FoundItem::getId).toList();
        }

        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (itemIds != null) {
            wrapper.in(Dispute::getItemId, itemIds);
        }'''
assert patch(be, [(old, new, 'listForPoint')], 'backend')

# ---------- 前端 ----------
fe = r'D:\codex\codex-data\schoollose\frontend\src\views\PointAdminView.vue'
pairs = [
(
'''      <el-alert
        v-if="!myPoints.length"''',
'''      <el-alert
        v-if="isSysAdmin"
        type="info"
        :closable="false"
        style="margin-bottom:10px"
        title="系统管理员视角：本页等同于站点管理员的全部权限 —— 下拉可切换任意投放点，做巡检核对、下架、确认归还入库；「纠纷核验」页签展示全部点位工单（站点负责人未在岗时可代填核验）。" />

      <el-alert
        v-if="!myPoints.length"''',
'admin-alert'),
(
'''            <template #empty>
              <el-empty v-if="!myPoints.length" description="当前账号未绑定投放点，看不到任何点位工单">''',
'''            <template #empty>
              <el-empty v-if="isSysAdmin" description="全部点位当前没有符合条件的纠纷工单，可清空筛选或点「刷新」再试" />
              <el-empty v-else-if="!myPoints.length" description="当前账号未绑定投放点，看不到任何点位工单">''',
'admin-empty'),
(
'''import { DISPUTE_TYPE_MAP } from '@/types'
import type { DropPoint } from '@/types\'''',
'''import { DISPUTE_TYPE_MAP } from '@/types'
import type { DropPoint } from '@/types'
import { useUserStore } from '@/stores/user\'''',
'import-store'),
(
'''const myPoints = ref<DropPoint[]>([])''',
'''const userStore = useUserStore()
const isSysAdmin = computed(() => userStore.isAdmin)
const myPoints = ref<DropPoint[]>([])''',
'store-init'),
]
assert patch(fe, pairs, 'frontend')
print('done')
