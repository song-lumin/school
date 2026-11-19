# -*- coding: utf-8 -*-
import io
p = r'D:\codex\codex-data\schoollose\frontend\src\views\PointAdminView.vue'
s = io.open(p, encoding='utf-8').read()

old_alert = '      <el-row :gutter="12" class="stat-row">'
new_alert = '''      <el-alert
        v-if="!myPoints.length"
        type="warning"
        :closable="false"
        style="margin-bottom:10px"
        title="当前账号没有绑定任何投放点：本页只显示点位负责人自己点位上的物品与纠纷；系统管理员请到「纠纷管理」查看全部工单并定责。" />

      <el-row :gutter="12" class="stat-row">'''
print('alert match:', s.count(old_alert))
if s.count(old_alert) == 1:
    s = s.replace(old_alert, new_alert, 1)

old_empty = '            <template #empty><el-empty description="本点位暂无纠纷工单" /></template>'
new_empty = '''            <template #empty>
              <el-empty v-if="!myPoints.length" description="当前账号未绑定投放点，看不到任何点位工单">
                <div class="sub-text" style="line-height:1.8;max-width:520px">
                  站点核验请用点位负责人账号登录：pointadmin1（周敏）负责图书馆 / 宿舍区 / 体育馆，pointadmin2（吴涛）负责学生活动中心 / 教学楼。<br>
                  系统管理员请到「纠纷管理」查看全部工单并定责。
                </div>
              </el-empty>
              <el-empty v-else description="当前点位没有符合条件的纠纷工单，可清空筛选或点「刷新」再试" />
            </template>'''
print('empty match:', s.count(old_empty))
if s.count(old_empty) == 1:
    s = s.replace(old_empty, new_empty, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('done')
